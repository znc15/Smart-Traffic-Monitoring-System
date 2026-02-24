package com.smarttraffic.backend.service;

import com.smarttraffic.backend.dto.auth.UpdatePasswordRequest;
import com.smarttraffic.backend.dto.auth.UpdateProfileRequest;
import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.model.UserEntity;
import com.smarttraffic.backend.repository.UserRepository;
import com.smarttraffic.backend.security.CurrentUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void updatePassword(CurrentUser currentUser, UpdatePasswordRequest request) {
        UserEntity user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Could not validate credentials"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "当前密码不正确！");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Transactional
    public void updateProfile(CurrentUser currentUser, UpdateProfileRequest request) {
        UserEntity user = userRepository.findById(currentUser.id())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Could not validate credentials"));

        if (StringUtils.hasText(request.getUsername()) && !request.getUsername().equals(user.getUsername())) {
            if (userRepository.existsByUsernameAndIdNot(request.getUsername(), user.getId())) {
                throw new AppException(HttpStatus.BAD_REQUEST, "用户名已存在！");
            }
            user.setUsername(request.getUsername());
        }

        if (StringUtils.hasText(request.getEmail()) && !request.getEmail().equals(user.getEmail())) {
            if (userRepository.existsByEmailAndIdNot(request.getEmail(), user.getId())) {
                throw new AppException(HttpStatus.BAD_REQUEST, "邮箱已被使用！");
            }
            user.setEmail(request.getEmail());
        }

        if (StringUtils.hasText(request.getPhoneNumber()) && !request.getPhoneNumber().equals(user.getPhoneNumber())) {
            if (userRepository.existsByPhoneNumberAndIdNot(request.getPhoneNumber(), user.getId())) {
                throw new AppException(HttpStatus.BAD_REQUEST, "手机号已被使用！");
            }
            user.setPhoneNumber(request.getPhoneNumber());
        }

        userRepository.save(user);
    }
}
