package com.smarttraffic.backend.security;

import com.smarttraffic.backend.model.UserEntity;

public record CurrentUser(
        Long id,
        String username,
        String email,
        String phoneNumber,
        Integer roleId
) {
    public static final int ADMIN_ROLE_ID = 0;
    public static final int USER_ROLE_ID = 1;

    public static CurrentUser fromEntity(UserEntity user) {
        return new CurrentUser(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getPhoneNumber(),
                user.getRoleId()
        );
    }

    public boolean isAdmin() {
        return roleId != null && roleId == ADMIN_ROLE_ID;
    }
}
