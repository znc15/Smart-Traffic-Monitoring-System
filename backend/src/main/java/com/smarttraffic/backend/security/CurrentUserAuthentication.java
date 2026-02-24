package com.smarttraffic.backend.security;

import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public class CurrentUserAuthentication extends AbstractAuthenticationToken {

    private final CurrentUser currentUser;
    private final String token;

    public CurrentUserAuthentication(CurrentUser currentUser, String token) {
        super(currentUser.isAdmin()
                ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN"), new SimpleGrantedAuthority("ROLE_USER"))
                : List.of(new SimpleGrantedAuthority("ROLE_USER")));
        this.currentUser = currentUser;
        this.token = token;
        setAuthenticated(true);
    }

    @Override
    public Object getCredentials() {
        return token;
    }

    @Override
    public Object getPrincipal() {
        return currentUser;
    }
}
