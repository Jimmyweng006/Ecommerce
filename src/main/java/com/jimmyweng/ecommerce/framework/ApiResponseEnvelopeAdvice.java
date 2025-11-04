package com.jimmyweng.ecommerce.framework;

import com.jimmyweng.ecommerce.controller.common.ApiResponseEnvelope;
import java.time.Instant;
import java.util.Collections;
import java.util.Map;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice
public class ApiResponseEnvelopeAdvice implements ResponseBodyAdvice<Object> {

    private static final String[] EXCLUDED_PATHS = {
        "/v3/api-docs",
        "/v3/api-docs.yaml"
    };

    @Override
    public boolean supports(
            @NonNull MethodParameter returnType, @NonNull Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(
            Object body,
            @NonNull MethodParameter returnType,
            @NonNull MediaType selectedContentType,
            @NonNull Class<? extends HttpMessageConverter<?>> selectedConverterType,
            @NonNull ServerHttpRequest request,
            @NonNull ServerHttpResponse response) {

        if (shouldBypass(request) || body instanceof ApiResponseEnvelope) {
            return body;
        }

        HttpStatus status = extractStatus(response);
        if (!status.is2xxSuccessful() || status == HttpStatus.NO_CONTENT) {
            return body;
        }

        Object payload = body == null ? Collections.emptyMap() : body;
        Map<String, Object> meta = Map.of("timestamp", Instant.now().toString());
        return new ApiResponseEnvelope(0, "OK", payload, meta);
    }

    private boolean shouldBypass(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        for (String excluded : EXCLUDED_PATHS) {
            if (path.startsWith(excluded)) {
                return true;
            }
        }
        return false;
    }

    private HttpStatus extractStatus(ServerHttpResponse response) {
        if (response instanceof ServletServerHttpResponse servletResponse) {
            return HttpStatus.valueOf(servletResponse.getServletResponse().getStatus());
        }
        return HttpStatus.OK;
    }
}
