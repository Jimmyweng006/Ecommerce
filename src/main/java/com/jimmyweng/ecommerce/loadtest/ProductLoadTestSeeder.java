package com.jimmyweng.ecommerce.loadtest;

import com.jimmyweng.ecommerce.model.product.Product;
import com.jimmyweng.ecommerce.repository.product.ProductRepository;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("load-test")
public class ProductLoadTestSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductLoadTestSeeder.class);
    private static final String TITLE_PREFIX = "LoadTest-Product-";
    private static final List<String> CATEGORIES = List.of("games", "books", "collectibles", "gadgets", "home");
    private static final List<String> KEYWORDS = List.of("board", "space", "retro", "limited", "flash", "sale", "top");

    private final ProductRepository productRepository;
    private final ProductSeedProperties properties;
    private final Random random = new SecureRandom();

    public ProductLoadTestSeeder(ProductRepository productRepository, ProductSeedProperties properties) {
        this.productRepository = productRepository;
        this.properties = properties;
    }

    @Override
    @Transactional
    public void run(String... args) {
        int count = properties.getSeedCount();
        log.info("Load-test profile active: seeding {} products with prefix '{}'", count, TITLE_PREFIX);

        productRepository.deleteByTitleStartingWith(TITLE_PREFIX);

        int batchSize = Math.max(1, properties.getBatchSize());
        List<Product> batch = new ArrayList<>(batchSize);
        for (int i = 0; i < count; i++) {
            String keyword = randomKeyword();
            String category = randomCategory();
            Product product = new Product(
                    TITLE_PREFIX + keyword + "-" + i,
                    "Synthetic " + keyword + " item " + i,
                    category,
                    randomPrice(),
                    randomStock());
            batch.add(product);

            if (batch.size() == batchSize) {
                productRepository.saveAll(batch);
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            productRepository.saveAll(batch);
        }

        log.info("Load-test seed completed.");
    }

    private String randomCategory() {
        return CATEGORIES.get(random.nextInt(CATEGORIES.size()));
    }

    private String randomKeyword() {
        return KEYWORDS.get(random.nextInt(KEYWORDS.size()));
    }

    private BigDecimal randomPrice() {
        double value = 5 + (200 - 5) * random.nextDouble();
        return BigDecimal.valueOf(Math.round(value * 100.0) / 100.0);
    }

    private int randomStock() {
        return 10 + random.nextInt(90);
    }
}
