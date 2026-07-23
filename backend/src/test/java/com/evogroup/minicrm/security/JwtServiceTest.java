package com.evogroup.minicrm.security;

import com.evogroup.minicrm.model.User;
import com.evogroup.minicrm.model.UserRole;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-at-least-32-bytes-long-for-hs256";

    private User user(String username) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setEmail("alice@example.com");
        user.setPasswordHash("hash");
        user.setRole(UserRole.MANAGER);
        return user;
    }

    @Test
    void generateToken_thenExtractUsername_roundTrips() {
        JwtService service = new JwtService(SECRET, 60_000);
        String token = service.generateToken(user("alice"));

        assertThat(service.isValid(token)).isTrue();
        assertThat(service.extractUsername(token)).isEqualTo("alice");
    }

    @Test
    void isValid_returnsFalse_forExpiredToken() {
        JwtService service = new JwtService(SECRET, -1_000);
        String token = service.generateToken(user("alice"));

        assertThat(service.isValid(token)).isFalse();
    }

    @Test
    void isValid_returnsFalse_forTamperedToken() {
        JwtService service = new JwtService(SECRET, 60_000);
        String token = service.generateToken(user("alice"));

        assertThat(service.isValid(token + "tampered")).isFalse();
    }

    @Test
    void isValid_returnsFalse_forTokenSignedWithDifferentSecret() {
        JwtService issuer = new JwtService(SECRET, 60_000);
        JwtService verifier = new JwtService("a-completely-different-secret-key-32-bytes-min", 60_000);
        String token = issuer.generateToken(user("alice"));

        assertThat(verifier.isValid(token)).isFalse();
    }
}
