package com.smarttraffic.backend.security;

public record CurrentUser(
        Long id,
        String username,
        String email,
        String phoneNumber,
        Integer roleId
) {
    public boolean isAdmin() {
        return roleId != null && roleId == 0;
    }
}
