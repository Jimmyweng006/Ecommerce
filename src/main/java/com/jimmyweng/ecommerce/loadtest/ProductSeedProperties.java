package com.jimmyweng.ecommerce.loadtest;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "loadtest")
public class ProductSeedProperties {

    /**
     * Number of synthetic products to seed for load testing.
     */
    private int seedCount = 1000;

    /**
     * Batch size used when persisting synthetic products.
     */
    private int batchSize = 200;
}
