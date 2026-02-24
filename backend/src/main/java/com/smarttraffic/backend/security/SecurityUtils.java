package com.smarttraffic.backend.security;

import com.smarttraffic.backend.exception.AppException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
    private SecurityUtils() {
    }

    public static CurrentUser requireCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CurrentUser currentUser)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }
        return currentUser;
    }
}
