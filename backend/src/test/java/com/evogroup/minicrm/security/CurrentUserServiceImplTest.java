package com.evogroup.minicrm.security;

import com.evogroup.minicrm.model.User;
import com.evogroup.minicrm.model.UserRole;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.assertThat;

class CurrentUserServiceImplTest {

    private final CurrentUserServiceImpl service = new CurrentUserServiceImpl();

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUser_returnsPrincipalFromSecurityContext() {
        User user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setRole(UserRole.MANAGER);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));

        User result = service.getCurrentUser();

        assertThat(result).isSameAs(user);
        assertThat(result.getUsername()).isEqualTo("alice");
    }
}
