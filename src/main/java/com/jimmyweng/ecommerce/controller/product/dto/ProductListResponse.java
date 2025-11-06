package com.jimmyweng.ecommerce.controller.product.dto;

import com.jimmyweng.ecommerce.model.product.Product;
import java.util.List;
import org.springframework.data.domain.Page;

public record ProductListResponse(List<ProductResponse> items, PageMetadata pagination) {

    public static ProductListResponse from(Page<Product> page) {
        List<ProductResponse> responses = page.getContent().stream()
                .map(ProductResponse::from)
                .toList();
        PageMetadata metadata = new PageMetadata(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext(),
                page.hasPrevious());
        return new ProductListResponse(responses, metadata);
    }
}
