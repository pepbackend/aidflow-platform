package com.aidflow.campaign.application;

import java.util.Set;
import java.util.UUID;

public record AuthenticatedUser(
        UUID id,
        String email,
        Set<String> roles
) {
    public boolean hasAnyRole(String... allowedRoles) {
        for (String allowedRole : allowedRoles) {
            if (roles.contains(allowedRole)) {
                return true;
            }
        }
        return false;
    }
}
