package com.jimmyweng.ecommerce.controller.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jimmyweng.ecommerce.constant.ErrorMessages;
import com.jimmyweng.ecommerce.constant.Role;
import com.jimmyweng.ecommerce.controller.auth.dto.AuthResponse;
import com.jimmyweng.ecommerce.controller.auth.dto.LoginRequest;
import com.jimmyweng.ecommerce.controller.common.ApiResponseEnvelope;
import com.jimmyweng.ecommerce.model.User;
import com.jimmyweng.ecommerce.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        User user = new User("customer@example.com", passwordEncoder.encode("password"), Role.USER);
        userRepository.save(user);
    }

    @Test
    void login_shouldReturnJwtToken() throws Exception {
        String payload =
                objectMapper.writeValueAsString(new LoginRequest("customer@example.com", "password"));

        String response = mockMvc
                .perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ret_code").value(0))
                .andExpect(jsonPath("$.msg").value("OK"))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.meta.timestamp").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        ApiResponseEnvelope apiResponseEnvelope = objectMapper.readValue(response, ApiResponseEnvelope.class);
        assertThat((((Map<String, String>) apiResponseEnvelope.data()).get("token"))).isNotBlank();
    }

    @Test
    void login_withInvalidCredentials_shouldReturnUnauthorized() throws Exception {
        String payload =
                objectMapper.writeValueAsString(new LoginRequest("customer@example.com", "wrong"));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.ret_code").value(-1))
                .andExpect(jsonPath("$.msg").value(ErrorMessages.AUTHENTICATION_FAILED));
    }
}
