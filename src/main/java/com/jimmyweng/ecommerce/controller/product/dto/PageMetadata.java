package com.jimmyweng.ecommerce.controller.product.dto;

public record PageMetadata(
        int page, int size, long totalElements, int totalPages, boolean hasNext, boolean hasPrevious) {}
