package com.jimmyweng.ecommerce.controller.order;

import static com.jimmyweng.ecommerce.constant.ErrorMessages.outOfStock;
import static com.jimmyweng.ecommerce.testsupport.TestAuthUtils.obtainToken;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jimmyweng.ecommerce.constant.ErrorMessages;
import com.jimmyweng.ecommerce.constant.OrderStatus;
import com.jimmyweng.ecommerce.constant.Role;
import com.jimmyweng.ecommerce.model.User;
import com.jimmyweng.ecommerce.model.order.Order;
import com.jimmyweng.ecommerce.model.order.OrderItem;
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

    private User defaultCustomer;

    @BeforeEach
    void setUp() {
        defaultCustomer = userRepository.save(new User("customer@example.com",
                passwordEncoder.encode("password"), Role.USER));
    }

    @Test
    void createOrder_whenInventorySufficient_persistOrderAndAdjustStock() throws Exception {
        Product product = productRepository.save(
                new Product("Board Game", "Co-op adventure", "games", new BigDecimal("79.99"), 10));

        String token = obtainToken(mockMvc, objectMapper, "customer@example.com", "password");
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
        assertEquals(8, updated.getStock());
        assertEquals(1, orderRepository.count());
        Order order = orderRepository.findAll().getFirst();
        assertEquals(1, order.getItems().size());
    }

    @Test
    void createOrder_whenStockInsufficient_returnConflict() throws Exception {
        Product product = productRepository.save(
                new Product("Limited Edition", "Rare", "collectibles", new BigDecimal("100.00"), 1));

        String token = obtainToken(mockMvc, objectMapper, "customer@example.com", "password");
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
    void createOrder_whenIdempotencyKeyReused_returnExistingOrder() throws Exception {
        Product product = productRepository.save(
                new Product("Miniatures", "Bundle", "games", new BigDecimal("50.00"), 5));

        String token = obtainToken(mockMvc, objectMapper, "customer@example.com", "password");
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

        assertEquals(1, orderRepository.count());
        Product updated = productRepository.findById(product.getId()).orElseThrow();
        assertEquals(4, updated.getStock());
    }

    @Test
    void createOrder_whenDifferentUsersSubmitUnorderedItems_shouldNotCauseDeadLock() throws Exception {
        Product firstProduct = productRepository.save(
                new Product("Desk Lamp", "Modern", "home", new BigDecimal("39.99"), 5));
        Product secondProduct = productRepository.save(
                new Product("Standing Desk", "Ergonomic", "office", new BigDecimal("299.99"), 5));

        User anotherUser =
                new User("second@example.com", passwordEncoder.encode("password"), Role.USER);
        userRepository.save(anotherUser);

        String firstToken = obtainToken(mockMvc, objectMapper, "customer@example.com", "password");
        String secondToken = obtainToken(mockMvc, objectMapper, "second@example.com", "password");

        String firstOrderPayload = objectMapper.writeValueAsString(Map.of(
                "idempotencyKey", UUID.randomUUID().toString(),
                "items",
                List.of(
                        Map.of("productId", firstProduct.getId(), "quantity", 1),
                        Map.of("productId", secondProduct.getId(), "quantity", 1))));

        String secondOrderPayload = objectMapper.writeValueAsString(Map.of(
                "idempotencyKey", UUID.randomUUID().toString(),
                "items",
                List.of(
                        Map.of("productId", secondProduct.getId(), "quantity", 1),
                        Map.of("productId", firstProduct.getId(), "quantity", 1))));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + firstToken)
                        .content(firstOrderPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ret_code").value(0));

        mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Authorization", "Bearer " + secondToken)
                        .content(secondOrderPayload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ret_code").value(0));

        Product firstUpdated = productRepository.findById(firstProduct.getId()).orElseThrow();
        Product secondUpdated = productRepository.findById(secondProduct.getId()).orElseThrow();
        assertEquals(3, firstUpdated.getStock());
        assertEquals(3, secondUpdated.getStock());
        assertEquals(2, orderRepository.count());
    }

    @Test
    void getOrder_whenOwnerRequests_returnsOrderDetails() throws Exception {
        Product product = productRepository.save(
                new Product("Collector Item", "Owner view", "collectibles", new BigDecimal("149.99"), 2));
        Order order = createOrder(defaultCustomer, product, 1, "owner-key");

        String token = obtainToken(mockMvc, objectMapper, defaultCustomer.getEmail(), "password");

        mockMvc.perform(get("/api/v1/orders/{orderId}", order.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ret_code").value(0))
                .andExpect(jsonPath("$.data.id").value(order.getId()))
                .andExpect(jsonPath("$.data.items[0].productId").value(product.getId()));
    }

    @Test
    void getOrder_whenAdminRequestsAnotherUserOrder_returnsOrderDetails() throws Exception {
        Product product = productRepository.save(
                new Product("Admin View", "Admin access", "misc", new BigDecimal("59.99"), 3));
        Order order = createOrder(defaultCustomer, product, 2, "admin-key");

        User admin =
                userRepository.save(new User("admin@example.com", passwordEncoder.encode("password"), Role.ADMIN));

        String token = obtainToken(mockMvc, objectMapper, admin.getEmail(), "password");

        mockMvc.perform(get("/api/v1/orders/{orderId}", order.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ret_code").value(0))
                .andExpect(jsonPath("$.data.id").value(order.getId()))
                .andExpect(jsonPath("$.data.items[0].productId").value(product.getId()));
    }

    @Test
    void getOrder_whenDifferentUserRequests_returnsNotFound() throws Exception {
        Product product = productRepository.save(
                new Product("Protected", "Should hide", "misc", new BigDecimal("29.99"), 1));
        Order order = createOrder(defaultCustomer, product, 1, "hidden-key");

        User otherUser =
                userRepository.save(new User("intruder@example.com", passwordEncoder.encode("password"), Role.USER));
        String token = obtainToken(mockMvc, objectMapper, otherUser.getEmail(), "password");

        mockMvc.perform(get("/api/v1/orders/{orderId}", order.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.ret_code").value(-1))
                .andExpect(jsonPath("$.msg").value(ErrorMessages.orderNotFound(order.getId())));
    }

    @Test
    void getOrder_whenOrderMissing_returnsNotFound() throws Exception {
        String token = obtainToken(mockMvc, objectMapper, defaultCustomer.getEmail(), "password");

        mockMvc.perform(get("/api/v1/orders/{orderId}", 9999L)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.ret_code").value(-1))
                .andExpect(jsonPath("$.msg").value(ErrorMessages.orderNotFound(9999L)));
    }

    private Order createOrder(User owner, Product product, int quantity, String key) {
        Order order = new Order(owner, OrderStatus.COMPLETED, key,
                product.getPrice().multiply(BigDecimal.valueOf(quantity)));
        order.addItem(new OrderItem(product, quantity, product.getPrice()));

        return orderRepository.saveAndFlush(order);
    }
}
