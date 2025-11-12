package com.jimmyweng.ecommerce.config.datasource;

import jakarta.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.readreplicas")
public class ReadReplicaProperties {

    private static final Logger log = LoggerFactory.getLogger(ReadReplicaProperties.class);

    /**
     * Spring's ConfigurationProperties binder requires a mutable list
     * so it can replace the entire collection (no final field).
     */
    private List<Replica> nodes = new ArrayList<>();

    public List<Replica> getNodes() {
        return nodes;
    }

    public void setNodes(List<Replica> nodes) {
        this.nodes = nodes == null ? new ArrayList<>() : new ArrayList<>(nodes);
    }

    public static class Replica {
        private String name;
        private String url;
        private String username;
        private String password;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public boolean isConfigured() {
            return StringUtils.hasText(this.url);
        }
    }

    @PostConstruct
    void logConfiguration() {
        log.debug("Replica configuration initialized with {} slot(s)", nodes.size());
        for (int i = 0; i < nodes.size(); i++) {
            Replica replica = nodes.get(i);
            log.debug(
                    "Replica slot {} -> name='{}', urlPresent={}, usernamePresent={}",
                    i,
                    replica.getName(),
                    StringUtils.hasText(replica.getUrl()),
                    StringUtils.hasText(replica.getUsername()));
        }
    }
}
