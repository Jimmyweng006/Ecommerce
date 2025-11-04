package com.jimmyweng.ecommerce.service.order.dto;

import java.util.List;

public record CreateOrderCommand(String idempotencyKey, List<OrderItemCommand> items) {}
