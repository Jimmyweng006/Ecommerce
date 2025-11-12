#!/bin/bash
set -euo pipefail

PRIMARY_HOST="${PRIMARY_HOST:-db-primary}"
PRIMARY_PORT="${PRIMARY_PORT:-3306}"
REPL_USER="${MYSQL_REPLICATION_USER:-repl_user}"
REPL_PASSWORD="${MYSQL_REPLICATION_PASSWORD:-repl_password}"

echo "Waiting for primary ${PRIMARY_HOST}:${PRIMARY_PORT} to accept replication connections..."
until mysql -h"${PRIMARY_HOST}" -P"${PRIMARY_PORT}" -u"${REPL_USER}" "-p${REPL_PASSWORD}" -e "SELECT 1" >/dev/null 2>&1; do
  sleep 3
done

echo "Configuring replica to follow ${PRIMARY_HOST}:${PRIMARY_PORT}"
mysql -uroot -p"${MYSQL_ROOT_PASSWORD}" <<SQL
STOP REPLICA;
RESET SLAVE ALL;
CHANGE REPLICATION SOURCE TO
  SOURCE_HOST='${PRIMARY_HOST}',
  SOURCE_PORT=${PRIMARY_PORT},
  SOURCE_USER='${REPL_USER}',
  SOURCE_PASSWORD='${REPL_PASSWORD}',
  SOURCE_AUTO_POSITION=1,
  GET_MASTER_PUBLIC_KEY=1;
START REPLICA;
SET GLOBAL super_read_only=ON;
SQL
