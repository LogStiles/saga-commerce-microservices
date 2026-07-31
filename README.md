# saga-commerce-microservices

An event-driven e-commerce order-processing backend implementing the **Saga orchestration
pattern**. Placing an order triggers a saga across services — **payment** then **inventory** —
coordinated by the `order-service` orchestrator. If a step fails (invalid credit card, or not
enough stock), the orchestrator runs **compensating transactions** to unwind the completed steps.

Modeled on [semotpan/saga-orchestration](https://github.com/semotpan/saga-orchestration),
reskinned from hotel booking to online-store ordering.

## Architecture

```
                 ┌───────────────────────── order-service (orchestrator) ─────────────────────────┐
   POST /orders  │  REST → save Order → SagaState → publish step command to outbox table          │
  ───────────────▶  Debezium CDC ──▶ payment.inbox.events / inventory.inbox.events                 │
                 │  ◀── payment.outbox.events / inventory.outbox.events ── Debezium CDC             │
                 └────────────────────────────────────────────────────────────────────────────────┘
                                │                                   │
                    payment-service                        inventory-service
             (fails if card ends in 1234)         (REJECTED if stock insufficient)
```

- **Transactional outbox + Debezium CDC**: every service writes events to a local `outboxevent`
  table in the same DB transaction as its business change. Debezium tails each DB's WAL and the
  outbox `EventRouter` SMT publishes the rows to Kafka — no dual-write, no lost events.
- **Idempotent consumers**: each service records processed event ids in an `eventlog` table and
  skips duplicates.
- **Dead-letter queues**: each consumer retries a failing/poison record twice (1s apart), then
  routes it to `<topic>-dlt` so the main flow keeps moving.
- Each service owns its **own Postgres database**.

### Kafka topic contract

| Hop | aggregateType | Topic |
|---|---|---|
| order → payment | `payment` | `payment.inbox.events` |
| order → inventory | `inventory` | `inventory.inbox.events` |
| payment → order | `payment` | `payment.outbox.events` |
| inventory → order | `inventory` | `inventory.outbox.events` |
| poison messages | — | `<topic>-dlt` |

## Tech

Java 25 · Spring Boot 4.1 · Spring for Apache Kafka (Jackson 3) · PostgreSQL 16 · Debezium 2.7 ·
Flyway · Docker Compose. Modules: `outbox` (shared library), `order-service`, `payment-service`,
`inventory-service`.

## Run it

```bash
docker compose up --build
```

This starts Kafka (KRaft mode — no ZooKeeper), Kafka Connect (Debezium), three Postgres databases, the three
services, and a one-shot `connector-setup` container that registers the Debezium connectors.

Service ports: order `8080`, payment `8081`, inventory `8082` (all under context path `/api`).
Kafka Connect REST: `http://localhost:8083`.

Check the connectors are running:

```bash
curl -s http://localhost:8083/connectors
```

## Try the saga

Seeded stock (`inventory-service/.../V99__demodata.sql`): item **1** (25 in stock), item **2**
(100), item **3** (0 — always out of stock). Payment **fails** for any card ending in `1234`.

**Happy path** — valid card, in-stock item → order `SUCCEED`:

```bash
curl -i -X POST http://localhost:8080/api/v1/orders \
  -H 'Content-Type: application/json' --data @e2e/order-placement.json
```

Copy the `Location` header id and poll status:

```bash
curl -s http://localhost:8080/api/v1/orders/<orderId>
```

**Payment failure / compensation** — card ends in `1234` → payment `FAILED`, order `FAILED`:

```bash
curl -i -X POST http://localhost:8080/api/v1/orders \
  -H 'Content-Type: application/json' --data @e2e/invalid-payment.json
```

**Insufficient stock / compensation** — item 3 (0 stock) → inventory `REJECTED`, payment is
compensated (`CANCELLED`), order `FAILED`:

```bash
curl -i -X POST http://localhost:8080/api/v1/orders \
  -H 'Content-Type: application/json' --data @e2e/insufficient-stock.json
```

## Useful commands

```bash
scripts/list-kafka-topics.sh                          # list topics
scripts/consume-kafka-topic.sh payment.inbox.events   # tail a topic (or a *-dlt topic)
./register-connectors.sh                              # re-register connectors after edits
```

## Build / test locally (without Docker)

```bash
./mvnw clean install          # build all modules (needs JDK 25)
./mvnw -pl order-service spring-boot:run   # run one service against local infra
```
