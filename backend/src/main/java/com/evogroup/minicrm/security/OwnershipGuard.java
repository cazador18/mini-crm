package com.evogroup.minicrm.security;

import com.evogroup.minicrm.model.Client;
import com.evogroup.minicrm.model.User;
import com.evogroup.minicrm.model.UserRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

@Component
public class OwnershipGuard {

    public void check(User currentUser, Client client) {
        if (currentUser.getRole() == UserRole.MANAGER
                && !client.getOwner().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Not the owner of this resource");
        }
    }
}
