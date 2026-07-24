package com.evogroup.minicrm.security;

import com.evogroup.minicrm.model.Client;
import com.evogroup.minicrm.model.User;
import com.evogroup.minicrm.model.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnershipGuardTest {

    private final OwnershipGuard guard = new OwnershipGuard();

    private User owner;
    private User otherManager;
    private User admin;
    private User viewer;
    private Client client;

    @BeforeEach
    void setUp() {
        owner = new User();
        owner.setId(1L);
        owner.setRole(UserRole.MANAGER);

        otherManager = new User();
        otherManager.setId(2L);
        otherManager.setRole(UserRole.MANAGER);

        admin = new User();
        admin.setId(100L);
        admin.setRole(UserRole.ADMIN);

        viewer = new User();
        viewer.setId(200L);
        viewer.setRole(UserRole.VIEWER);

        client = new Client();
        client.setId(1L);
        client.setOwner(owner);
    }

    @Test
    void check_passes_forAdmin_evenWhenNotOwner() {
        assertThatCode(() -> guard.check(admin, client)).doesNotThrowAnyException();
    }

    @Test
    void check_passes_forViewer_evenWhenNotOwner() {
        assertThatCode(() -> guard.check(viewer, client)).doesNotThrowAnyException();
    }

    @Test
    void check_passes_forManager_whenOwner() {
        assertThatCode(() -> guard.check(owner, client)).doesNotThrowAnyException();
    }

    @Test
    void check_throwsAccessDenied_forManager_whenNotOwner() {
        assertThatThrownBy(() -> guard.check(otherManager, client))
                .isInstanceOf(AccessDeniedException.class);
    }
}
