#!/usr/bin/env bash
#
# Registers (or updates) the three Debezium outbox connectors against a running
# Kafka Connect instance. docker-compose already does this via the "connector-setup"
# service; use this script to re-register after editing a connector config.
#
set -euo pipefail
CONNECT_URL="${CONNECT_URL:-http://localhost:8083}"

register() {
  local name="$1" file="$2"
  echo ">> $name"
  curl -sf -X PUT -H 'Content-Type: application/json' \
    --data "@${file}" \
    "${CONNECT_URL}/connectors/${name}/config" >/dev/null
  echo "   registered"
}

register order-outbox-connector     order-service/order-outbox-connector.json
register payment-outbox-connector   payment-service/payment-outbox-connector.json
register inventory-outbox-connector inventory-service/inventory-outbox-connector.json

echo "Done. Current connectors:"
curl -sf "${CONNECT_URL}/connectors" && echo
