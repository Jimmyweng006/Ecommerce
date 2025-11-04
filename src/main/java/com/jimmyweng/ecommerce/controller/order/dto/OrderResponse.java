package com.jimmyweng.ecommerce.controller.order.dto;

import com.jimmyweng.ecommerce.model.order.Order;
import com.jimmyweng.ecommerce.model.order.OrderItem;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        @Schema(example = "10") Long id,
        @Schema(example = "COMPLETED") String status,
        @Schema(example = "159.98") BigDecimal totalAmount,
        @Schema(example = "2024-01-01T00:00:00Z") Instant createdAt,
        List<OrderItemResponse> items) {

    public static OrderResponse from(Order order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(OrderResponse::toResponse)
                .toList();
        return new OrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                itemResponses);
    }

    private static OrderItemResponse toResponse(OrderItem item) {
        return new OrderItemResponse(
                item.getProduct().getId(),
                item.getProduct().getTitle(),
                item.getQuantity(),
                item.getUnitPrice());
    }
}
