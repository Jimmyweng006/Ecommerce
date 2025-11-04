package com.jimmyweng.ecommerce.controller.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record CreateOrderRequest(
        @NotBlank String idempotencyKey,
        @NotEmpty List<@Valid OrderItemRequest> items) {}
