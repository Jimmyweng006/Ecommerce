package com.jimmyweng.ecommerce.controller.favorite;

import static com.jimmyweng.ecommerce.testsupport.TestAuthUtils.obtainToken;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jimmyweng.ecommerce.constant.ErrorMessages;
import com.jimmyweng.ecommerce.constant.Role;
import com.jimmyweng.ecommerce.controller.favorite.dto.AddFavoriteRequest;
import com.jimmyweng.ecommerce.model.User;
import com.jimmyweng.ecommerce.model.favorite.Favorite;
import com.jimmyweng.ecommerce.model.favorite.FavoriteId;
import com.jimmyweng.ecommerce.model.product.Product;
import com.jimmyweng.ecommerce.repository.UserRepository;
import com.jimmyweng.ecommerce.repository.favorite.FavoriteRepository;
import com.jimmyweng.ecommerce.repository.product.ProductRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
class FavoriteControllerIntegrationTests {

    private static final String USER_EMAIL = "customer@example.com";
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
    private FavoriteRepository favoriteRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long userId;

    @BeforeEach
    void setUp() {
        User saved = userRepository.save(new User(USER_EMAIL, passwordEncoder.encode(USER_PASSWORD), Role.USER));
        userId = saved.getId();
    }

    @Test
    void addFavorite_whenProductExists_returnCreatedWithProductPayload() throws Exception {
        Product product = productRepository.save(
                new Product("Tower Defence", "Fun strategy", "games", new BigDecimal("19.99"), 10));

        String token = obtainToken(mockMvc, objectMapper, USER_EMAIL, USER_PASSWORD);
        AddFavoriteRequest request = new AddFavoriteRequest(product.getId());

        mockMvc.perform(post("/api/v1/favorites")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ret_code").value(0))
                .andExpect(jsonPath("$.data.id").value(product.getId()))
                .andExpect(jsonPath("$.data.title").value("Tower Defence"));

        assertTrue(favoriteRepository.existsById(new FavoriteId(userId, product.getId())));
    }

    @Test
    void addFavorite_whenProductAlreadyFavorited_returnOk() throws Exception {
        Product product = productRepository.save(
                new Product("Board Game", "Co-op adventure", "games", new BigDecimal("59.99"), 5));
        favoriteRepository.save(new Favorite(userId, product.getId(), Instant.now()));

        String token = obtainToken(mockMvc, objectMapper, USER_EMAIL, USER_PASSWORD);
        AddFavoriteRequest request = new AddFavoriteRequest(product.getId());

        mockMvc.perform(post("/api/v1/favorites")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ret_code").value(0))
                .andExpect(jsonPath("$.data.id").value(product.getId()));

        List<Favorite> favorites = favoriteRepository.findAllByIdUserIdOrderByCreatedAtDesc(userId);
        assertEquals(1, favorites.size());
    }

    @Test
    void listFavorites_whenFavoritesExist_returnProductsInOrder() throws Exception {
        Product latest = productRepository.save(
                new Product("Sci-Fi", "Latest addition", "books", new BigDecimal("24.99"), 3));
        Product earlier = productRepository.save(
                new Product("Classic", "Earlier favorite", "books", new BigDecimal("14.99"), 7));

        favoriteRepository.save(new Favorite(userId, earlier.getId(), Instant.now().minusSeconds(10)));
        favoriteRepository.save(new Favorite(userId, latest.getId(), Instant.now()));

        String token = obtainToken(mockMvc, objectMapper, USER_EMAIL, USER_PASSWORD);

        mockMvc.perform(get("/api/v1/favorites")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ret_code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(latest.getId()))
                .andExpect(jsonPath("$.data[1].id").value(earlier.getId()));
    }

    @Test
    void addFavorite_whenProductMissing_returnNotFound() throws Exception {
        String token = obtainToken(mockMvc, objectMapper, USER_EMAIL, USER_PASSWORD);
        AddFavoriteRequest request = new AddFavoriteRequest(9999L);

        mockMvc.perform(post("/api/v1/favorites")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.ret_code").value(-1))
                .andExpect(jsonPath("$.msg").value(ErrorMessages.productNotFound(9999L)));
    }

    @Test
    void removeFavorite_whenFavoriteExists_returnNoContent() throws Exception {
        Product product = productRepository.save(
                new Product("Accessory", "Must have", "gadgets", new BigDecimal("9.99"), 15));
        favoriteRepository.save(new Favorite(userId, product.getId(), Instant.now()));

        String token = obtainToken(mockMvc, objectMapper, USER_EMAIL, USER_PASSWORD);

        mockMvc.perform(delete("/api/v1/favorites/{productId}", product.getId())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());

        assertTrue(favoriteRepository.findAllByIdUserIdOrderByCreatedAtDesc(userId).isEmpty());
    }

}
