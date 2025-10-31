package com.jimmyweng.ecommerce.service.product.dto;

import java.math.BigDecimal;

public record UpdateProductCommand(
        String title, String description, String category, BigDecimal price, Integer stock) {}
