package com.jimmyweng.ecommerce.controller.order;

import static com.jimmyweng.ecommerce.constant.ErrorMessages.outOfStock;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jimmyweng.ecommerce.constant.Role;
import com.jimmyweng.ecommerce.controller.auth.dto.LoginRequest;
import com.jimmyweng.ecommerce.controller.common.ApiResponseEnvelope;
import com.jimmyweng.ecommerce.model.User;
import com.jimmyweng.ecommerce.model.order.Order;
import com.jimmyweng.ecommerce.model.product.Product;
import com.jimmyweng.ecommerce.repository.UserRepository;
import com.jimmyweng.ecommerce.repository.order.OrderRepository;
import com.jimmyweng.ecommerce.repository.product.ProductRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        User user = new User("customer@example.com", passwordEncoder.encode("password"), Role.USER);
        userRepository.save(user);
    }

    @Test
    void create_order_decrements_stock_and_persists_items() throws Exception {
        Product product = productRepository.save(
                new Product("Board Game", "Co-op adventure", "games", new BigDecimal("79.99"), 10));

        String token = obtainToken("customer@example.com", "password");
        String payload = objectMapper.writeValueAsString(Map.of(
                "idempotencyKey", UUID.randomUUID().toString(),
                "items", List.of(Map.of("productId", product.getId(), "quantity", 2))));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ret_code").value(0))
                .andExpect(jsonPath("$.data.items[0].productId").value(product.getId()))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.meta.timestamp").exists());

        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(8);
        assertThat(orderRepository.count()).isEqualTo(1);
        Order order = orderRepository.findAll().getFirst();
        assertThat(order.getItems()).hasSize(1);
    }

    @Test
    void create_order_returns_conflict_when_stock_insufficient() throws Exception {
        Product product = productRepository.save(
                new Product("Limited Edition", "Rare", "collectibles", new BigDecimal("100.00"), 1));

        String token = obtainToken("customer@example.com", "password");
        String payload = objectMapper.writeValueAsString(Map.of(
                "idempotencyKey", UUID.randomUUID().toString(),
                "items", List.of(Map.of("productId", product.getId(), "quantity", 2))));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.ret_code").value(-1))
                .andExpect(jsonPath("$.msg").value(outOfStock(product.getId())));
    }

    @Test
    void create_order_is_idempotent() throws Exception {
        Product product = productRepository.save(
                new Product("Miniatures", "Bundle", "games", new BigDecimal("50.00"), 5));

        String token = obtainToken("customer@example.com", "password");
        String key = UUID.randomUUID().toString();
        String payload = objectMapper.writeValueAsString(Map.of(
                "idempotencyKey", key,
                "items", List.of(Map.of("productId", product.getId(), "quantity", 1))));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ret_code").value(0));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + token)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ret_code").value(0));

        assertThat(orderRepository.count()).isEqualTo(1);
        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertThat(updated.getStock()).isEqualTo(4);
    }

    private String obtainToken(String email, String password) throws Exception {
        String response = mockMvc
                .perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ApiResponseEnvelope envelope = objectMapper.readValue(response, ApiResponseEnvelope.class);
        @SuppressWarnings("unchecked")
        Map<String, String> data = (Map<String, String>) envelope.data();
        return data.get("token");
    }
}
