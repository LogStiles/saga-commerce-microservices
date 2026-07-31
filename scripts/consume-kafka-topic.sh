#!/usr/bin/env bash
# Tail a Kafka topic from the beginning, printing key + headers + value.
# Usage: scripts/consume-kafka-topic.sh <topic>   (e.g. payment.inbox.events, inventory.outbox.events, payment.inbox.events-dlt)
set -euo pipefail
TOPIC="${1:?usage: consume-kafka-topic.sh <topic>}"
docker exec -t kafka kafka-console-consumer \
  --bootstrap-server kafka:9092 \
  --topic "$TOPIC" \
  --from-beginning \
  --property print.key=true \
  --property print.headers=true
