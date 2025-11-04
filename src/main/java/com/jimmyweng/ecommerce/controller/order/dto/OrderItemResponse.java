package com.jimmyweng.ecommerce.controller.order.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;

public record OrderItemResponse(
        @Schema(example = "1") Long productId,
        @Schema(example = "Board Game") String title,
        @Schema(example = "2") Integer quantity,
        @Schema(example = "79.99") BigDecimal unitPrice) {}
