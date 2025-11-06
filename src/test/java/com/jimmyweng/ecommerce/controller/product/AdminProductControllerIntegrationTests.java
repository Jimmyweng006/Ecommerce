package com.jimmyweng.ecommerce.controller.product;

import static com.jimmyweng.ecommerce.testsupport.TestAuthUtils.obtainToken;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jimmyweng.ecommerce.constant.ErrorMessages;
import com.jimmyweng.ecommerce.constant.Role;
import com.jimmyweng.ecommerce.controller.product.dto.CreateProductRequest;
import com.jimmyweng.ecommerce.controller.product.dto.UpdateProductRequest;
import com.jimmyweng.ecommerce.model.User;
import com.jimmyweng.ecommerce.model.product.Product;
import com.jimmyweng.ecommerce.repository.UserRepository;
import com.jimmyweng.ecommerce.repository.product.ProductRepository;
import jakarta.transaction.Transactional;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdminProductControllerIntegrationTests {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String ADMIN_PASSWORD = "password";
    private static final String USER_EMAIL = "user@example.com";
    private static final String USER_PASSWORD = "password";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        User admin = new User(ADMIN_EMAIL, passwordEncoder.encode(ADMIN_PASSWORD), Role.ADMIN);
        userRepository.saveAndFlush(admin);
    }

    @Test
    void createProduct_whenPayloadValid_returnCreatedResponse() throws Exception {
        String token = obtainToken(mockMvc, objectMapper, ADMIN_EMAIL, ADMIN_PASSWORD);
        CreateProductRequest request =
                new CreateProductRequest(
                        "Board Game",
                        "Cooperative sci-fi adventure",
                        "board-games",
                        new BigDecimal("79.99"),
                        50);

        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ret_code").value(0))
                .andExpect(jsonPath("$.msg").value("OK"))
                .andExpect(jsonPath("$.data.title").value("Board Game"))
                .andExpect(jsonPath("$.data.category").value("board-games"))
                .andExpect(jsonPath("$.data.price").value(79.99))
                .andExpect(jsonPath("$.data.version").value(0))
                .andExpect(jsonPath("$.meta.timestamp").exists());

        assertEquals(1, productRepository.count());
    }

    @Test
    void createProduct_whenPayloadInvalid_returnBadRequest() throws Exception {
        String token = obtainToken(mockMvc, objectMapper, ADMIN_EMAIL, ADMIN_PASSWORD);
        CreateProductRequest request =
                new CreateProductRequest(
                        "",
                        "Desc",
                        "misc",
                        new BigDecimal("10.00"),
                        1);

        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ret_code").value(-1))
                .andExpect(jsonPath("$.msg").exists());
    }


    @Test
    void updateProduct_whenPayloadValid_returnUpdatedResponse() throws Exception {
        String token = obtainToken(mockMvc, objectMapper, ADMIN_EMAIL, ADMIN_PASSWORD);
        Product saved = productRepository.saveAndFlush(
                new Product(
                        "Old Title",
                        "Old description",
                        "board-games",
                        new BigDecimal("49.99"),
                        10));

        UpdateProductRequest request =
                new UpdateProductRequest(
                        "New Title",
                        "New description",
                        "strategy",
                        new BigDecimal("59.99"),
                        25,
                        saved.getVersion());

        mockMvc.perform(put("/api/v1/admin/products/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ret_code").value(0))
                .andExpect(jsonPath("$.msg").value("OK"))
                .andExpect(jsonPath("$.data.title").value("New Title"))
                .andExpect(jsonPath("$.data.category").value("strategy"))
                .andExpect(jsonPath("$.data.price").value(59.99))
                .andExpect(jsonPath("$.data.version").value(1))
                .andExpect(jsonPath("$.meta.timestamp").exists());

        Product updated = productRepository.findById(saved.getId()).orElseThrow();
        assertEquals("New Title", updated.getTitle());
        assertEquals("New description", updated.getDescription());
        assertEquals("strategy", updated.getCategory());
        assertEquals(new BigDecimal("59.99"), updated.getPrice());
        assertEquals(25, updated.getStock());
    }

    @Test
    void updateProduct_whenTargetMissing_returnNotFound() throws Exception {
        String token = obtainToken(mockMvc, objectMapper, ADMIN_EMAIL, ADMIN_PASSWORD);
        UpdateProductRequest request =
                new UpdateProductRequest("Missing",
                        "Will fail",
                        "misc",
                        new BigDecimal("9.99"),
                        1,
                        0L);

        mockMvc.perform(put("/api/v1/admin/products/{id}", 9999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.ret_code").value(-1))
                .andExpect(jsonPath("$.msg").value(ErrorMessages.productNotFound(9999L)));
    }

    @Test
    void updateProduct_whenVersionStale_returnConflict() throws Exception {
        String token = obtainToken(mockMvc, objectMapper, ADMIN_EMAIL, ADMIN_PASSWORD);
        Product saved = productRepository.saveAndFlush(
                new Product(
                        "Hot Item",
                        "Initial",
                        "misc",
                        new BigDecimal("29.99"),
                        10));
        long staleVersion = saved.getVersion();

        // Simulate another admin updating the product first
        saved.applyUpdate(
                "Hot Item",
                "Updated description",
                "misc",
                new BigDecimal("29.99"),
                9);
        productRepository.saveAndFlush(saved);

        UpdateProductRequest request =
                new UpdateProductRequest(
                        "Hot Item",
                        "Conflicting update",
                        "misc",
                        new BigDecimal("27.99"),
                        8,
                        staleVersion);

        mockMvc.perform(put("/api/v1/admin/products/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.ret_code").value(-1))
                .andExpect(jsonPath("$.msg").value(ErrorMessages.RESOURCE_MODIFIED));
    }

    @Test
    void deleteProduct_whenAdminAuthorized_markSoftDeleted() throws Exception {
        String token = obtainToken(mockMvc, objectMapper, ADMIN_EMAIL, ADMIN_PASSWORD);
        Product saved = productRepository.saveAndFlush(
                new Product(
                        "Delete Me",
                        "To be removed",
                        "misc",
                        new BigDecimal("19.99"),
                        5));

        mockMvc.perform(delete("/api/v1/admin/products/{id}", saved.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        Product deleted = productRepository.findById(saved.getId()).orElseThrow();
        assertNotNull(deleted.getDeletedAt());
        assertTrue(deleted.getDeletedAt().isAfter(
                deleted.getCreatedAt()) || deleted.getDeletedAt().equals(deleted.getCreatedAt()));
    }

    @Test
    void deleteProduct_whenUserNotAdmin_returnForbidden() throws Exception {
        Product saved = productRepository.saveAndFlush(
                new Product(
                        "Protected",
                        "Only admins can delete",
                        "misc",
                        new BigDecimal("9.99"),
                        1));

        User nonAdmin = new User(USER_EMAIL, passwordEncoder.encode(USER_PASSWORD), Role.USER);
        userRepository.saveAndFlush(nonAdmin);

        String token = obtainToken(mockMvc, objectMapper, USER_EMAIL, USER_PASSWORD);

        mockMvc.perform(delete("/api/v1/admin/products/{id}", saved.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

}
