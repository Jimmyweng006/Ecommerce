package com.jimmyweng.ecommerce.controller.common;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

@Schema(description = "Standard API response envelope")
public record ApiResponseEnvelope(
        @JsonProperty("ret_code")
        @Schema(description = "Application status code", example = "0") int retCode,
        @Schema(description = "Human readable message", example = "OK") String msg,
        @Schema(description = "Response payload") Object data,
        @Schema(description = "Additional metadata such as trace id or timestamp") Map<String, Object> meta) {
}
