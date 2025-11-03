package com.jimmyweng.ecommerce.controller.product.dto;

import com.jimmyweng.ecommerce.model.product.Product;

import java.math.BigDecimal;
import java.time.Instant;

public record ProductResponse(
        Long id,
        String title,
        String description,
        String category,
        BigDecimal price,
        Integer stock,
        Instant createdAt,
        Instant updatedAt,
        Instant deletedAt,
        Long version) {

    public static ProductResponse from(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getTitle(),
                product.getDescription(),
                product.getCategory(),
                product.getPrice(),
                product.getStock(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                product.getDeletedAt(),
                product.getVersion());
    }
}
