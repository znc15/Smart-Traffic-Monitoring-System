package com.smarttraffic.backend.service;

import com.smarttraffic.backend.dto.auth.LoginResponse;
import com.smarttraffic.backend.dto.auth.RegisterRequest;
import com.smarttraffic.backend.dto.auth.UserResponse;
import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.model.UserEntity;
import com.smarttraffic.backend.repository.UserRepository;
import com.smarttraffic.backend.security.CurrentUser;
import com.smarttraffic.backend.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public void register(RegisterRequest request) {
        boolean conflict = userRepository.findByUsername(request.getUsername()).isPresent()
                || userRepository.findByEmail(request.getEmail()).isPresent()
                || userRepository.findByPhoneNumber(request.getPhoneNumber()).isPresent();

        if (conflict) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Username, email hoặc số điện thoại đã tồn tại!");
        }

        UserEntity entity = new UserEntity();
        entity.setUsername(request.getUsername());
        entity.setEmail(request.getEmail());
        entity.setPhoneNumber(request.getPhoneNumber());
        entity.setRoleId(userRepository.count() == 0 ? 0 : 1);
        entity.setPassword(passwordEncoder.encode(request.getPassword()));
        userRepository.save(entity);
    }

    public LoginResponse login(String email, String password) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Sai thông tin đăng nhập"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Sai thông tin đăng nhập");
        }

        if (!user.isEnabled()) {
            throw new AppException(HttpStatus.FORBIDDEN, "Account is disabled");
        }

        CurrentUser currentUser = toCurrentUser(user);
        String token = jwtService.createAccessToken(currentUser);
        return new LoginResponse(token, "bearer");
    }

    public UserResponse me(CurrentUser currentUser) {
        UserEntity user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Could not validate credentials"));
        return toUserResponse(user);
    }

    public UserEntity requireUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Could not validate credentials"));
    }

    public CurrentUser toCurrentUser(UserEntity user) {
        return new CurrentUser(user.getId(), user.getUsername(), user.getEmail(), user.getPhoneNumber(), user.getRoleId());
    }

    private UserResponse toUserResponse(UserEntity user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhoneNumber(user.getPhoneNumber());
        response.setRoleId(user.getRoleId());
        return response;
    }
}
