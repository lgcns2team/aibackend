package com.lgcns.haibackend.common.security;

import java.util.UUID;

import org.springframework.security.core.Authentication;

public final class AuthUtils {
    private AuthUtils() {}

    public static UUID getUserId(Authentication auth) {
        UUID userId = UUID.fromString(auth.getPrincipal().toString());
        return userId;
    }

    public static boolean hasRole(Authentication auth, String role) {
        return auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals(role) || a.getAuthority().equals("ROLE_" + role));
    }
}
