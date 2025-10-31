package com.jimmyweng.ecommerce.controller.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record UpdateProductRequest(
        @NotBlank @Size(max = 255) String title,
        @Size(max = 2000) String description,
        @NotBlank @Size(max = 100) String category,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal price,
        @NotNull @Min(0) Integer stock) {}
