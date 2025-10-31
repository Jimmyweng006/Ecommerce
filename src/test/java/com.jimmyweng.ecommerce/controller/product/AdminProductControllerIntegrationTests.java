package com.jimmyweng.ecommerce.controller.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jimmyweng.ecommerce.constant.Role;
import com.jimmyweng.ecommerce.controller.auth.dto.LoginRequest;
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
        productRepository.deleteAll();
        userRepository.deleteAll();

        User admin = new User(ADMIN_EMAIL, passwordEncoder.encode(ADMIN_PASSWORD), Role.ADMIN);
        userRepository.save(admin);
    }

    @Test
    void create_product_returns_created_response() throws Exception {
        String token = obtainToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        CreateProductRequest request =
                new CreateProductRequest("Board Game", "Cooperative sci-fi adventure", "board-games", new BigDecimal("79.99"), 50);

        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Board Game"))
                .andExpect(jsonPath("$.category").value("board-games"))
                .andExpect(jsonPath("$.price").value(79.99));

        assertEquals(1, productRepository.count());
    }

    @Test
    void update_product_returns_updated_payload() throws Exception {
        String token = obtainToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        Product saved = productRepository.save(
                new Product("Old Title", "Old description", "board-games", new BigDecimal("49.99"), 10));

        UpdateProductRequest request =
                new UpdateProductRequest("New Title", "New description", "strategy", new BigDecimal("59.99"), 25);

        mockMvc.perform(put("/api/v1/admin/products/{id}", saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("New Title"))
                .andExpect(jsonPath("$.category").value("strategy"))
                .andExpect(jsonPath("$.price").value(59.99));

        Product updated = productRepository.findById(saved.getId()).orElseThrow();
        assertEquals("New Title", updated.getTitle());
        assertEquals("New description", updated.getDescription());
        assertEquals("strategy", updated.getCategory());
        assertEquals(new BigDecimal("59.99"), updated.getPrice());
        assertEquals(25, updated.getStock());
    }

    @Test
    void delete_product_marks_soft_delete() throws Exception {
        String token = obtainToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        Product saved = productRepository.save(
                new Product("Delete Me", "To be removed", "misc", new BigDecimal("19.99"), 5));

        mockMvc.perform(delete("/api/v1/admin/products/{id}", saved.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        Product deleted = productRepository.findById(saved.getId()).orElseThrow();
        assertNotNull(deleted.getDeletedAt());
        assertTrue(deleted.getDeletedAt().isAfter(deleted.getCreatedAt()) || deleted.getDeletedAt().equals(deleted.getCreatedAt()));
    }

    @Test
    void update_nonexistent_product_returns_not_found() throws Exception {
        String token = obtainToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        UpdateProductRequest request =
                new UpdateProductRequest("Missing", "Will fail", "misc", new BigDecimal("9.99"), 1);

        mockMvc.perform(put("/api/v1/admin/products/{id}", 9999L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.ret_code").value(-1))
                .andExpect(jsonPath("$.msg").value("Product not found: 9999"));
    }

    @Test
    void create_product_with_invalid_payload_returns_bad_request() throws Exception {
        String token = obtainToken(ADMIN_EMAIL, ADMIN_PASSWORD);
        CreateProductRequest request =
                new CreateProductRequest("", "Desc", "misc", new BigDecimal("10.00"), 1);

        mockMvc.perform(post("/api/v1/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.ret_code").value(-1))
                .andExpect(jsonPath("$.msg").exists());
    }

    @Test
    void delete_product_with_non_admin_should_be_forbidden() throws Exception {
        Product saved = productRepository.save(
                new Product("Protected", "Only admins can delete", "misc", new BigDecimal("9.99"), 1));

        User nonAdmin = new User(USER_EMAIL, passwordEncoder.encode(USER_PASSWORD), Role.USER);
        userRepository.save(nonAdmin);

        String token = obtainToken(USER_EMAIL, USER_PASSWORD);

        mockMvc.perform(delete("/api/v1/admin/products/{id}", saved.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private String obtainToken(String email, String password) throws Exception {
        JsonNode response = objectMapper.readTree(mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString());
        return response.get("token").asText();
    }
}
