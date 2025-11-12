package com.jimmyweng.ecommerce.controller.product.dto;

public record SliceMetadata(int page, int size, int numberOfElements, boolean hasNext, boolean hasPrevious) {}
