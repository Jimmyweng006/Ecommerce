#!/usr/bin/env bash
set -euo pipefail
REPLICAS=${TARGET_REPLICAS:-"ecommerce-db-replica-1 ecommerce-db-replica-2 ecommerce-db-replica-3"}
TABLE=${TARGET_TABLE:-products}
INDEXES=${TARGET_INDEXES:-"idx_products_deleted_category_created idx_products_title_description_fulltext"}
DATABASE=${TARGET_DATABASE:-ecommerce}
for target in $REPLICAS; do
  echo "==== $target ===="
  docker exec "$target" mysql -uroot -prootpassword -e "SHOW REPLICA STATUS\\G" 2>/dev/null \
    | awk '/Replica_IO_Running|Replica_SQL_Running|Seconds_Behind_Source/ {print;}' || echo "Failed to query $target"
  echo
  for idx in $INDEXES; do
    docker exec "$target" mysql -uroot -prootpassword -D"$DATABASE" -e "SHOW INDEX FROM ${TABLE} WHERE Key_name='${idx}';" 2>/dev/null \
      | awk -v idx="$idx" 'NR==2 {printf "Index %s exists (column=%s)\n", idx, $5; found=1} END {if (NR<2) printf "Index %s NOT FOUND\n", idx}'
  done
  echo
 done
