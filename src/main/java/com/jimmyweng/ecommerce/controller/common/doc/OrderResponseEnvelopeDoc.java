package com.jimmyweng.ecommerce.controller.common.doc;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.jimmyweng.ecommerce.controller.order.dto.OrderResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(name = "OrderResponseEnvelope")
public record OrderResponseEnvelopeDoc(
        @JsonProperty("ret_code")
        @Schema(example = "0") int retCode,
        @Schema(example = "OK") String msg,
        @Schema(oneOf = OrderResponse.class) OrderResponse data,
        @Schema(example = "{'timestamp':'2024-01-01T00:00:00Z'}") Map<String, Object> meta) {}
