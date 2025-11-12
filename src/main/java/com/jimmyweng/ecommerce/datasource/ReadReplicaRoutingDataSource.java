package com.jimmyweng.ecommerce.datasource;

import com.jimmyweng.ecommerce.config.datasource.DataSourceRoutingConfiguration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

public class ReadReplicaRoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger log = LoggerFactory.getLogger(ReadReplicaRoutingDataSource.class);

    private final List<String> replicaKeys;
    private final AtomicInteger counter = new AtomicInteger();

    public ReadReplicaRoutingDataSource(List<String> replicaKeys) {
        this.replicaKeys = replicaKeys == null ? Collections.emptyList() : List.copyOf(replicaKeys);
    }

    @Override
    protected Object determineCurrentLookupKey() {
        boolean forcePrimary = ReplicaRoutingContext.isForcePrimary();
        boolean readOnlyTx = TransactionSynchronizationManager.isCurrentTransactionReadOnly();
        boolean hasReplicas = !replicaKeys.isEmpty();

        if (forcePrimary || !readOnlyTx || !hasReplicas) {
            if (log.isDebugEnabled()) {
                log.debug(
                        "Routing to PRIMARY (forcePrimary={}, readOnlyTx={}, replicasConfigured={})",
                        forcePrimary,
                        readOnlyTx,
                        hasReplicas);
            }
            return DataSourceRoutingConfiguration.PRIMARY_KEY;
        }

        int index = Math.abs(counter.getAndIncrement());
        String key = replicaKeys.get(index % replicaKeys.size());
        if (log.isDebugEnabled()) {
            log.debug(
                    "Routing read-only transaction to {} (forcePrimary={}, readOnlyTx=true, replicaCount={})",
                    key,
                    forcePrimary,
                    replicaKeys.size());
        }
        return key;
    }
}
