package com.jimmyweng.ecommerce.repository.favorite;

import com.jimmyweng.ecommerce.model.favorite.Favorite;
import com.jimmyweng.ecommerce.model.favorite.FavoriteId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, FavoriteId> {

    List<Favorite> findAllByIdUserIdOrderByCreatedAtDesc(Long userId);
}
