#!/usr/bin/env bash
set -euo pipefail

if [[ $# -eq 0 ]]; then
  echo "Usage: $0 <command to run load test>"
  echo "Example: CONTAINERS=\"ecommerce-app ecommerce-db-primary ecommerce-db-replica-1 ecommerce-db-replica-2 ecommerce-db-replica-3\" ./scripts/loadtest_with_stats.sh k6 run load-test/products-browse.js"
  exit 1
fi

CONTAINERS=${CONTAINERS:-"ecommerce-app ecommerce-db-primary ecommerce-db-replica-1 ecommerce-db-replica-2 ecommerce-db-replica-3"}
INTERVAL=${INTERVAL:-1}
DEFAULT_OUT="load-test/docker-stats-$(date +%Y%m%d-%H%M%S).log"
OUT=${OUT:-$DEFAULT_OUT}

if [[ -z "$CONTAINERS" ]]; then
  echo "CONTAINERS is empty. Set CONTAINERS=\"container1 container2\" to monitor specific containers."
  exit 1
fi

echo "Capturing docker stats for containers: $CONTAINERS"
echo "Interval: ${INTERVAL}s  Output: ${OUT}"

collect_stats() {
  while true; do
    local ts
    ts="$(date '+%Y-%m-%d %H:%M:%S')"
    docker stats --no-stream $CONTAINERS | sed "s/^/${ts} /"
    echo
    sleep "$INTERVAL"
  done
}

mkdir -p "$(dirname "$OUT")"

collect_stats > "$OUT" &
STATS_PID=$!

cleanup() {
  if ps -p "$STATS_PID" > /dev/null 2>&1; then
    kill "$STATS_PID" >/dev/null 2>&1 || true
  fi
  echo "Docker stats saved to $OUT"
}

trap cleanup EXIT

"$@"
