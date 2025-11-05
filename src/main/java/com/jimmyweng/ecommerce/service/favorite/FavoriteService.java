package com.jimmyweng.ecommerce.service.favorite;

import com.jimmyweng.ecommerce.constant.ErrorMessages;
import com.jimmyweng.ecommerce.exception.ResourceNotFoundException;
import com.jimmyweng.ecommerce.model.User;
import com.jimmyweng.ecommerce.model.favorite.Favorite;
import com.jimmyweng.ecommerce.model.favorite.FavoriteId;
import com.jimmyweng.ecommerce.model.product.Product;
import com.jimmyweng.ecommerce.repository.UserRepository;
import com.jimmyweng.ecommerce.repository.favorite.FavoriteRepository;
import com.jimmyweng.ecommerce.repository.product.ProductRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.jimmyweng.ecommerce.constant.ErrorMessages.userNotFound;

@Service
@Transactional
public class FavoriteService {

    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final FavoriteRepository favoriteRepository;
    private final Clock clock;

    public FavoriteService(
            UserRepository userRepository,
            ProductRepository productRepository,
            FavoriteRepository favoriteRepository,
            Clock clock) {
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.favoriteRepository = favoriteRepository;
        this.clock = clock;
    }

    public record AddFavoriteResult(Product product, boolean created) {}

    public AddFavoriteResult addFavorite(String userEmail, Long productId) {
        User user = loadUser(userEmail);
        Product product = productRepository
                .findByIdAndDeletedAtIsNull(productId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorMessages.productNotFound(productId)));

        FavoriteId id = new FavoriteId(user.getId(), product.getId());
        if (favoriteRepository.existsById(id)) {
            return new AddFavoriteResult(product, false);
        }

        Favorite favorite = new Favorite(user.getId(), product.getId(), Instant.now(clock));
        favoriteRepository.save(favorite);
        return new AddFavoriteResult(product, true);
    }

    @Transactional(readOnly = true)
    public List<Product> listFavorites(String userEmail) {
        User user = loadUser(userEmail);

        List<Favorite> favorites = favoriteRepository.findAllByIdUserIdOrderByCreatedAtDesc(user.getId());
        if (favorites.isEmpty()) {
            return List.of();
        }

        List<Long> productIds = favorites.stream()
                .map(Favorite::getProductId)
                .toList();
        Map<Long, Product> productsById = productRepository.findAllByIdInAndDeletedAtIsNull(productIds).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

        return favorites.stream()
                .map(favorite -> productsById.get(favorite.getProductId()))
                .filter(Objects::nonNull)
                .toList();
    }

    public void removeFavorite(String userEmail, Long productId) {
        User user = loadUser(userEmail);

        FavoriteId id = new FavoriteId(user.getId(), productId);
        if (favoriteRepository.existsById(id)) {
            favoriteRepository.deleteById(id);
        }
    }

    private User loadUser(String userEmail) {
        return userRepository
                .findByEmailIgnoreCase(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException(userNotFound(userEmail)));
    }
}
