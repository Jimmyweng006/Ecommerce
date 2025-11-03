package com.jimmyweng.ecommerce.controller.common;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(description = "Standard API response envelope used for both success and error flows")
public record ApiResponseEnvelope(
        @Schema(description = "Application specific status code", example = "0") int retCode,
        @Schema(description = "Human readable message", example = "OK") String msg,
        @Schema(description = "Primary response payload") Map<String, Object> data,
        @Schema(description = "Additional metadata such as trace id or timestamp")
                Map<String, Object> meta) {}
