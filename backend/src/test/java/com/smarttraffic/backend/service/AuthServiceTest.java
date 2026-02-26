package com.smarttraffic.backend.service;

import com.smarttraffic.backend.dto.auth.LoginResponse;
import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.model.UserEntity;
import com.smarttraffic.backend.repository.UserRepository;
import com.smarttraffic.backend.security.CurrentUser;
import com.smarttraffic.backend.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthServiceTest {

    @Test
    void login_shouldRejectDisabledAccount() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = new AuthService(userRepository, passwordEncoder, jwtService);

        UserEntity user = mockUser(true, false);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password@1", user.getPassword())).thenReturn(true);

        AppException ex = assertThrows(
                AppException.class,
                () -> authService.login("test@example.com", "Password@1")
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void login_shouldRejectWrongPassword() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = new AuthService(userRepository, passwordEncoder, jwtService);

        UserEntity user = mockUser(true, true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", user.getPassword())).thenReturn(false);

        AppException ex = assertThrows(
                AppException.class,
                () -> authService.login("test@example.com", "wrong")
        );
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void login_shouldReturnAccessTokenWhenCredentialsValid() {
        UserRepository userRepository = mock(UserRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        JwtService jwtService = mock(JwtService.class);
        AuthService authService = new AuthService(userRepository, passwordEncoder, jwtService);

        UserEntity user = mockUser(true, true);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password@1", user.getPassword())).thenReturn(true);
        when(jwtService.createAccessToken(any(CurrentUser.class))).thenReturn("jwt-token");

        LoginResponse response = authService.login("test@example.com", "Password@1");

        assertEquals("jwt-token", response.getAccessToken());
        assertEquals("bearer", response.getTokenType());
    }

    private static UserEntity mockUser(boolean adminRole, boolean enabled) {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setUsername("tester");
        user.setEmail("test@example.com");
        user.setPhoneNumber("13800000000");
        user.setPassword("encoded-password");
        user.setEnabled(enabled);
        user.setRoleId(adminRole ? 0 : 1);
        return user;
    }
}
