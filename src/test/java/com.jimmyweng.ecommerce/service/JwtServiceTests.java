package com.jimmyweng.ecommerce.service;

import com.jimmyweng.ecommerce.service.auth.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTests {

    private static final String UNIT_TEST_SECRET =
            "unit-test-secret-unit-test-secret-unit-test-secret";
    private JwtService jwtService;
    private final Clock clock = Clock.fixed(Instant.parse("2024-01-01T00:00:00Z"), ZoneOffset.UTC);

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(UNIT_TEST_SECRET, 3600, clock);
    }

    @Test
    void generateToken_shouldEmbedSubjectAndRoles() {
        UserDetails userDetails =
                User.withUsername("tester@example.com").password("password").roles("USER").build();

        String token = jwtService.generateToken(userDetails);

        Claims claims = jwtService.parseClaims(token);
        assertEquals("tester@example.com", claims.getSubject());
        assertEquals(List.of("ROLE_USER"), claims.get("roles", List.class));
        assertTrue(jwtService.isTokenValid(token, userDetails));
    }

    @Test
    void parseClaims_shouldRejectInvalidToken() {
        assertThrows(IllegalArgumentException.class, () -> jwtService.parseClaims("bad-token"));
    }

    @Test
    void isTokenValid_shouldReturnFalseWhenTokenExpired() {
        UserDetails userDetails =
                User.withUsername("tester@example.com").password("password").roles("USER").build();

        JwtService shortLivedService = new JwtService(UNIT_TEST_SECRET, 1, clock);
        String token = shortLivedService.generateToken(userDetails);

        Clock futureClock = Clock.fixed(clock.instant().plusSeconds(5), ZoneOffset.UTC);
        JwtService futureService = new JwtService(UNIT_TEST_SECRET, 1, futureClock);

        IllegalArgumentException thrown =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> futureService.isTokenValid(token, userDetails));
        assertInstanceOf(ExpiredJwtException.class, thrown.getCause());
    }
}
