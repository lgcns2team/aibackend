package com.lgcns.haibackend.common.security;

import java.security.Principal;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

public final class AuthUtils {
    private AuthUtils() {}

    public static UUID getUserId(Authentication auth) {
        UUID userId = UUID.fromString(auth.getPrincipal().toString());
        return userId;
    }

    public static UUID getUserId(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new IllegalStateException("Principal is null");
        }
        return UUID.fromString(principal.getName());
    }

    public static boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
               .anyMatch(a -> a.getAuthority().equals(role) || a.getAuthority().equals("ROLE_" + role));
    }
}
