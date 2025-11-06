package com.jimmyweng.ecommerce.testsupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jimmyweng.ecommerce.controller.auth.dto.LoginRequest;
import com.jimmyweng.ecommerce.controller.common.ApiResponseEnvelope;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

public final class TestAuthUtils {

    private TestAuthUtils() {
    }

    @SuppressWarnings("unchecked")
    public static String obtainToken(MockMvc mockMvc, ObjectMapper objectMapper, String email, String password)
            throws Exception {
        String response = mockMvc
                .perform(MockMvcRequestBuilders.post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, password))))
                .andReturn()
                .getResponse()
                .getContentAsString();

        ApiResponseEnvelope envelope = objectMapper.readValue(response, ApiResponseEnvelope.class);
        return ((Map<String, String>) envelope.data()).get("token");
    }
}
