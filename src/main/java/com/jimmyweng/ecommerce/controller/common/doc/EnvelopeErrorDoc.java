package com.jimmyweng.ecommerce.controller.common.doc;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

@Schema(name = "ErrorEnvelope")
public record EnvelopeErrorDoc(
        @JsonProperty("ret_code")
        @Schema(example = "-1") int retCode,
        @Schema(example = "Validation failed") String msg,
        @Schema(example = "{}") Map<String, Object> data,
        @Schema(example = "{'timestamp':'2024-01-01T00:00:00Z'}") Map<String, Object> meta) {}
