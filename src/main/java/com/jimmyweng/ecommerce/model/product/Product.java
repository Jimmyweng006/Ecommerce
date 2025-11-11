package com.jimmyweng.ecommerce.model.product;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stock;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    public Product(String title, String description, String category, BigDecimal price, Integer stock) {
        this.title = title;
        this.description = description;
        this.category = normalizeCategory(category);
        this.price = price;
        this.stock = stock;
    }

    public void applyUpdate(String title, String description, String category, BigDecimal price, Integer stock) {
        this.title = title;
        this.description = description;
        this.category = normalizeCategory(category);
        this.price = price;
        this.stock = stock;
    }

    public void markDeleted(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
        this.category = normalizeCategory(this.category);
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
        this.category = normalizeCategory(this.category);
    }

    private static String normalizeCategory(String category) {
        return category == null ? null : category.trim().toLowerCase(Locale.ROOT);
    }
}
