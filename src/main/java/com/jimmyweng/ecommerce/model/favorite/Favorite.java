package com.jimmyweng.ecommerce.model.favorite;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "favorites")
public class Favorite {

    @EmbeddedId
    private FavoriteId id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Favorite(Long userId, Long productId, Instant createdAt) {
        this.id = new FavoriteId(userId, productId);
        this.createdAt = createdAt;
    }

    public Long getUserId() {
        return id.getUserId();
    }

    public Long getProductId() {
        return id.getProductId();
    }
}
