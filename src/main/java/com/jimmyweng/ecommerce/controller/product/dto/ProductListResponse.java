package com.jimmyweng.ecommerce.controller.product.dto;

import com.jimmyweng.ecommerce.model.product.Product;
import java.util.List;
import org.springframework.data.domain.Slice;

public record ProductListResponse(List<ProductResponse> items, SliceMetadata pagination) {

    public static ProductListResponse from(Slice<Product> slice) {
        List<ProductResponse> responses = slice.getContent().stream()
                .map(ProductResponse::from)
                .toList();
        SliceMetadata metadata = new SliceMetadata(
                slice.getNumber(),
                slice.getSize(),
                slice.getNumberOfElements(),
                slice.hasNext(),
                slice.hasPrevious());
        return new ProductListResponse(responses, metadata);
    }
}
