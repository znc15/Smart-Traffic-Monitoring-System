package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.config.JwtProperties;
import com.smarttraffic.backend.dto.auth.LoginResponse;
import com.smarttraffic.backend.dto.auth.RegisterRequest;
import com.smarttraffic.backend.dto.auth.RegisterResponse;
import com.smarttraffic.backend.dto.auth.UserResponse;
import com.smarttraffic.backend.service.AuthService;
import com.smarttraffic.backend.security.SecurityUtils;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    public AuthController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(new RegisterResponse("注册成功"));
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<LoginResponse> login(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpServletResponse response
    ) {
        LoginResponse loginResponse = authService.login(username, password);

        ResponseCookie cookie = ResponseCookie.from("access_token", loginResponse.getAccessToken())
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .path("/")
                .maxAge(Duration.ofDays(jwtProperties.getAccessTokenExpireDays()))
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ResponseEntity.ok(loginResponse);
    }

    @GetMapping("/me")
    public UserResponse me() {
        return authService.me(SecurityUtils.requireCurrentUser());
    }
}
