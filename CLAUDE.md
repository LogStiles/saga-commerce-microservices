# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

An event-driven e-commerce order-processing backend implementing the **Saga orchestration pattern**.
Placing an order runs a choreographed-by-orchestrator saga across two participants — **payment** then
**inventory** — with **compensating transactions** on failure. Reliability comes from the
**transactional outbox + Debezium CDC** pattern; consumers are idempotent; failed/poison messages go
to dead-letter topics.

## Stack (bleeding edge — do not "downgrade" to match tutorials)

Spring Boot **4.1**, Java **25**, Jackson **3** (`tools.jackson`, not `com.fasterxml.jackson.databind`),
Hibernate 7.4, PostgreSQL 16, Debezium 2.7, Flyway, Docker Compose. Consequences that bite:

- Kafka JSON (de)serialization **must** use spring-kafka's `JacksonJsonDeserializer` /
  `JacksonJsonSerializer` (Jackson 3). The legacy `JsonDeserializer` / `JsonSerializer` are
  Jackson-2-based and **will not compile** (Jackson 2 databind is absent).
- `KafkaProperties.buildConsumerProperties()` / `buildProducerProperties()` are **no-arg** in Boot 4.
- JSONB columns map with `@JdbcTypeCode(SqlTypes.JSON)` on Jackson-3 `JsonNode`/`ObjectNode`/`Object`
  fields (Hibernate 7.4's `Jackson3JsonFormatMapper` handles them) — **not** Hypersistence types.
- Jackson **annotations** still live in `com.fasterxml.jackson.annotation` (e.g. `@JsonIgnoreProperties`).
- A deserialization target must **not** have `final` fields. Jackson 3 constructs the object first
  (Lombok's `@NoArgsConstructor(force = true)` initialises finals to `null`) and then writes the
  properties — and it cannot write a `final` field. There is **no error**: the field silently stays
  `null` and you find out later, somewhere else. `Payment` hit exactly this — every `final` field
  arrived null and only its one mutable field was populated, which then failed `persist()` on a
  null `@Id`. Use non-final fields, or a `record` (filled via the canonical constructor, so finals
  are fine there). Entities need non-final fields for Hibernate to materialise rows anyway.
- Entity `@Table(name=...)` must be **lowercase, no camelCase** — Spring's naming strategy converts
  `eventLog` → `event_log`, which won't match the `eventlog` migrations. `order` is a reserved word,
  so the order table is `orders`.

## Build / run / test

Requires JDK 25. There is no global Maven; use the root wrapper. If `JAVA_HOME` points elsewhere,
set it for the build:

```bash
export JAVA_HOME="/c/Program Files/Eclipse Adoptium/jdk-25.0.3.9-hotspot"   # example on this machine
./mvnw clean install                 # reactor build; outbox is built before the services
./mvnw -q -DskipTests install        # compile only (see test note below)
./mvnw -pl order-service -am -DskipTests package   # build one service + its deps (outbox)
```

`./mvnw` at the root aggregates the four Maven modules. `outbox` is an upstream dependency of the
three services, so it must be installed first — the reactor handles ordering.

Run the whole system (Kafka, Debezium, three Postgres DBs, three services, connector registration):

```bash
docker compose up --build
```

Ports: order `8080`, payment `8081`, inventory `8082` (context path `/api`); Kafka Connect `8083`;
DBs `5432`/`5433`/`5434`. See `README.md` for the three end-to-end scenarios and the payloads in `e2e/`.

**Tests:** the `*ApplicationTests` are `@SpringBootTest` context-load tests that need a live DB + Kafka
(no Testcontainers is configured). They fail on a bare `mvn install`; build with `-DskipTests` unless
the Docker stack (or equivalent infra) is up. Single test once infra exists:
`./mvnw -pl payment-service test -Dtest=PaymentApplicationTests`.

## Architecture

Four Maven modules: `outbox` (shared library), `order-service` (orchestrator + REST entry point),
`payment-service`, `inventory-service` (participants). There is no shipping service.

### Saga flow (orchestration, not choreography)

`order-service` owns the saga. `POST /api/v1/orders` → `ItemOrderUseCase` saves an `Order` and calls
`TransactionManager.begin`, which persists a `TransactionState` and drives a `TransactionSaga`.

- `framework/` is a **generic saga engine**: `Transaction` (idempotency via `ensureProcessed` + an
  `eventlog` table), `TransactionState` (JPA-persisted saga state machine: JSONB `payload`, per-step
  `step_status`, and `advanceTransactionStatus()` computing STARTED/ABORTING/ABORTED/FINISHED).
- `saga/TransactionSaga` runs the state machine: `advance()` publishes a REQUEST command to the next
  step; on failure `goBack()` publishes a CANCEL (compensation) command to the previous step.
  `saga/TransactionStateOrder` defines the step order: **PAYMENT → INVENTORY** (compensate backward).
- Participants (`payment-service`, `inventory-service`) are dumb: consume a command, do their work
  atomically with a DB write, publish a result event via the outbox. Idempotency via their own
  `eventlog` table (`EventLogs.isAlreadyProcessed` / `processed`).

### Transactional outbox + Debezium CDC (how events actually move)

No service produces to Kafka directly for saga events. Each publishes a domain event via
`ApplicationEventPublisher`; `outbox/OutboxEventDispatcher` (an `@EventListener` with
`@Transactional(MANDATORY)`) writes it to the local `outboxevent` table **in the same transaction** as
the business change. Debezium tails each DB's WAL and its outbox `EventRouter` SMT publishes rows to
Kafka (`*-outbox-connector.json` per service). The `outboxevent` column names (`id`, `aggregatetype`,
`aggregateid`, `type`, `payload`, `timestamp`) match Debezium EventRouter defaults exactly — don't rename them.

### Topic contract (must stay consistent end to end)

EventRouter routes each row to `<aggregateType>.<suffix>.events`; the orchestrator connector uses
suffix `.inbox`, the participant connectors use `.outbox`.

| Hop | aggregateType | Topic |
|---|---|---|
| order → payment | `payment` | `payment.inbox.events` |
| order → inventory | `inventory` | `inventory.inbox.events` |
| payment → order | `payment` | `payment.outbox.events` |
| inventory → order | `inventory` | `inventory.outbox.events` |

The orchestrator's step names (`TransactionStateOrder`) ARE the participant aggregateTypes, so the
inventory result event's `aggregateType` must be `inventory` (not `item-order`). Message key =
`aggregateid` = the saga/transaction id; header `id` = the per-event outbox id used for idempotency;
header `eventType` = the outbox `type` column.

### Cross-service JSON payload coupling (easy to break)

The orchestrator publishes the **same** `Order.toTransactionPayload()` JSON to each step's topic, and
each participant deserializes it into its own type. So the payload keys are a contract:
`payment-service` reads `{purchaseId, shopperId, paymentAmount, creditCardNum, type}` into its
`Payment` entity; `inventory-service` reads `{itemId, quantity, type}`. Both targets use
`@JsonIgnoreProperties(ignoreUnknown = true)`. Changing `Order.toTransactionPayload()` keys or a
participant's fields without keeping them aligned silently breaks deserialization.

### Failure triggers (for testing compensation)

Payment **fails** for any card ending in `1234` (`Payment.paymentStatus()`). Inventory **rejects**
when stock is insufficient; seed data (`inventory .../V99__demodata.sql`) gives item 3 zero stock.

### Migrations

Flyway, `classpath:db/migration`, versioned `V__`. `outbox` ships `V0__outbox_events.sql` **inside its
jar**, so the `outboxevent` table is created in every service that depends on `outbox` — a new
participant must depend on the `outbox` module for this to work.

### Dead-letter queues

Every consumer wraps deserializers in `ErrorHandlingDeserializer` and attaches a `DefaultErrorHandler`
with a `DeadLetterPublishingRecoverer` (retry twice, 1s apart) → `<topic>-dlt` (spring-kafka 4.x
default suffix; it was `.DLT` in 3.x).

The DLT `KafkaTemplate` must serialize **both** shapes, via `DelegatingByTypeSerializer` (`byte[]`
→ `ByteArraySerializer`, everything else → `JacksonJsonSerializer`). A *deserialization* failure
recovers raw bytes, but a *listener* failure recovers the already-deserialized object. A
`byte[]`-only template throws `ClassCastException` while publishing to the DLT, so the record can
neither be processed nor parked and the consumer retries it forever — which also blocks the poll
loop past `max.poll.interval.ms` and triggers endless rebalances.

Consumers set `enable-auto-commit=false`: the listener container owns offsets. Auto-commit advances
them on a timer regardless of listener outcome, defeating `DefaultErrorHandler`'s seek-based retries.
