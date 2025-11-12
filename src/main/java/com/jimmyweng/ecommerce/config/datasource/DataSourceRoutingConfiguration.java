package com.jimmyweng.ecommerce.config.datasource;

import com.jimmyweng.ecommerce.datasource.ReadReplicaRoutingDataSource;
import com.zaxxer.hikari.HikariDataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableConfigurationProperties(ReadReplicaProperties.class)
public class DataSourceRoutingConfiguration {

    private static final Logger log = LoggerFactory.getLogger(DataSourceRoutingConfiguration.class);

    public static final String PRIMARY_KEY = "PRIMARY";

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource primaryDataSource(DataSourceProperties properties) {
        return properties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
    }

    @Bean
    @Primary
    public DataSource dataSource(
            DataSourceProperties properties,
            ReadReplicaProperties readReplicaProperties,
            DataSource primaryDataSource,
            Environment environment) {

        List<ReadReplicaProperties.Replica> replicas = new ArrayList<>(readReplicaProperties.getNodes());
        if (replicas.isEmpty()) {
            replicas.addAll(loadReplicasFromEnvironment(environment, properties));
        }

        if (log.isDebugEnabled()) {
            log.debug("Primary datasource configured for URL={}", properties.determineUrl());
            log.debug("Replica slots detected: {}", replicas.size());
            for (int i = 0; i < replicas.size(); i++) {
                ReadReplicaProperties.Replica replica = replicas.get(i);
                log.debug("Replica slot {} -> name='{}', url='{}'", i, replica.getName(), replica.getUrl());
            }
        }

        Map<Object, Object> targetDataSources = new HashMap<>();
        targetDataSources.put(PRIMARY_KEY, primaryDataSource);

        List<String> replicaKeys = new ArrayList<>();
        AtomicInteger replicaIndex = new AtomicInteger(1);

        for (ReadReplicaProperties.Replica replica : replicas) {
            if (!replica.isConfigured()) {
                continue;
            }

            String key = "REPLICA_" + replicaIndex.getAndIncrement();
            DataSource replicaDataSource = buildReplicaDataSource(properties, replica);
            targetDataSources.put(key, replicaDataSource);
            replicaKeys.add(key);
        }

        if (log.isInfoEnabled() && primaryDataSource instanceof HikariDataSource hikariPrimary) {
            if (!StringUtils.hasText(hikariPrimary.getPoolName())) {
                hikariPrimary.setPoolName("PrimaryPool");
            }
            log.info(
                    "Primary Hikari pool initialized with maxPoolSize={} minIdle={} poolName={}",
                    hikariPrimary.getMaximumPoolSize(),
                    hikariPrimary.getMinimumIdle(),
                    hikariPrimary.getPoolName());
        }

        ReadReplicaRoutingDataSource routingDataSource = new ReadReplicaRoutingDataSource(replicaKeys);
        routingDataSource.setTargetDataSources(targetDataSources);
        routingDataSource.setDefaultTargetDataSource(primaryDataSource);
        routingDataSource.afterPropertiesSet();
        return new LazyConnectionDataSourceProxy(routingDataSource);
    }

    private DataSource buildReplicaDataSource(DataSourceProperties primaryProperties,
                                              ReadReplicaProperties.Replica replica) {

        DataSourceBuilder<HikariDataSource> builder = DataSourceBuilder.create().type(HikariDataSource.class);
        builder.url(replica.getUrl());
        builder.username(StringUtils.hasText(replica.getUsername())
                ? replica.getUsername()
                : primaryProperties.getUsername());
        builder.password(StringUtils.hasText(replica.getPassword())
                ? replica.getPassword()
                : primaryProperties.getPassword());

        if (StringUtils.hasText(primaryProperties.getDriverClassName())) {
            builder.driverClassName(primaryProperties.getDriverClassName());
        }

        HikariDataSource dataSource = builder.build();
        dataSource.setMaximumPoolSize(50);
        dataSource.setMinimumIdle(20);
        dataSource.setPoolName("ReplicaPool-" + replica.getName());

        if (log.isInfoEnabled()) {
            log.info(
                    "Replica Hikari pool initialized (name={}, maxPoolSize={}, minIdle={})",
                    dataSource.getPoolName(),
                    dataSource.getMaximumPoolSize(),
                    dataSource.getMinimumIdle());
        }
        return dataSource;
    }

    private List<ReadReplicaProperties.Replica> loadReplicasFromEnvironment(
            Environment environment, DataSourceProperties primaryProperties) {

        List<ReadReplicaProperties.Replica> replicas = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            String base = "app.readreplicas[" + i + "]";
            String url = environment.getProperty(base + ".url");
            if (!StringUtils.hasText(url)) {
                continue;
            }

            ReadReplicaProperties.Replica replica = new ReadReplicaProperties.Replica();
            replica.setName(environment.getProperty(base + ".name", "replica-" + (i + 1)));
            replica.setUrl(url);
            replica.setUsername(environment.getProperty(base + ".username", primaryProperties.getUsername()));
            replica.setPassword(environment.getProperty(base + ".password", primaryProperties.getPassword()));
            replicas.add(replica);
        }

        if (log.isDebugEnabled()) {
            log.debug("Loaded {} replica slot(s) from Environment fallback", replicas.size());
        }
        return replicas;
    }
}
