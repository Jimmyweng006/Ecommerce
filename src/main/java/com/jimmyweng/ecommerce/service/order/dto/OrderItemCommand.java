package com.jimmyweng.ecommerce.service.order.dto;

public record OrderItemCommand(Long productId, int quantity) {}
