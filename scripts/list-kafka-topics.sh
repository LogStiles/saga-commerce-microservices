#!/usr/bin/env bash
# List all Kafka topics in the running cluster.
set -euo pipefail
docker exec -t kafka kafka-topics --bootstrap-server kafka:9092 --list
