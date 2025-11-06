package com.jimmyweng.ecommerce.controller.product;

import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jimmyweng.ecommerce.constant.ErrorMessages;
import com.jimmyweng.ecommerce.model.product.Product;
import com.jimmyweng.ecommerce.repository.product.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ProductControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Test
    void listProducts_whenNoFilters_returnsActiveProductsOrderedByCreatedAt() throws Exception {
        Product older = productRepository.save(
                new Product("Retro Game", "Classic", "games", new BigDecimal("19.99"), 5));
        Product newer = productRepository.save(
                new Product("New Game", "Latest hit", "games", new BigDecimal("49.99"), 10));

        mockMvc.perform(get("/api/v1/products").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ret_code").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[*].id").value(hasItems(newer.getId().intValue(), older.getId().intValue())))
                .andExpect(jsonPath("$.data.pagination.totalElements").value(2));
    }

    @Test
    void listProducts_whenFilteringByCategory_returnsMatchingProducts() throws Exception {
        productRepository.saveAll(List.of(
                new Product("Space Novel", "Sci-fi", "books", new BigDecimal("14.99"), 20),
                new Product("Fantasy Epic", "Magic world", "books", new BigDecimal("24.99"), 15),
                new Product("Board Game", "Strategy", "games", new BigDecimal("39.99"), 10)));

        mockMvc.perform(get("/api/v1/products")
                        .param("category", "books")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ret_code").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(2))
                .andExpect(jsonPath("$.data.items[0].category").value("books"))
                .andExpect(jsonPath("$.data.items[1].category").value("books"))
                .andExpect(jsonPath("$.data.pagination.totalElements").value(2));
    }

    @Test
    void listProducts_whenSearchingByKeyword_matchesTitleOrDescription() throws Exception {
        productRepository.saveAll(List.of(
                new Product("Laser Sword", "Futuristic weapon", "gadgets", new BigDecimal("59.99"), 7),
                new Product("Decor Lamp", "Futuristic design", "home", new BigDecimal("29.99"), 12),
                new Product("Classic Chair", "Wooden chair", "home", new BigDecimal("89.99"), 4)));

        mockMvc.perform(get("/api/v1/products")
                        .param("search", "futuristic")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ret_code").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(2));
    }

    @Test
    void listProducts_deleteOneProduct_excludesSoftDeletedItems() throws Exception {
        Product active = productRepository.save(
                new Product("Premium Keyboard", "Mechanical", "electronics", new BigDecimal("129.99"), 8));
        Product deleted = productRepository.save(
                new Product("Old Mouse", "Legacy", "electronics", new BigDecimal("19.99"), 2));
        deleted.markDeleted(Instant.now());
        productRepository.save(deleted);

        mockMvc.perform(get("/api/v1/products").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ret_code").value(0))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].id").value(active.getId()));
    }

    @Test
    void getProduct_whenExists_returnsDetails() throws Exception {
        Product product = productRepository.save(
                new Product("Limited Edition", "Rare collectible", "collectibles", new BigDecimal("199.99"), 3));

        mockMvc.perform(get("/api/v1/products/{productId}", product.getId()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ret_code").value(0))
                .andExpect(jsonPath("$.data.id").value(product.getId()))
                .andExpect(jsonPath("$.data.title").value("Limited Edition"));
    }

    @Test
    void getProduct_whenDeleted_returnsNotFound() throws Exception {
        Product product = productRepository.save(
                new Product("Hidden Item", "Should not show", "misc", new BigDecimal("9.99"), 1));
        product.markDeleted(Instant.now());
        productRepository.save(product);

        mockMvc.perform(get("/api/v1/products/{productId}", product.getId()).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.ret_code").value(-1))
                .andExpect(jsonPath("$.msg").value(ErrorMessages.productNotFound(product.getId())));
    }
}
