package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.dto.auth.UpdatePasswordRequest;
import com.smarttraffic.backend.dto.auth.UpdateProfileRequest;
import com.smarttraffic.backend.dto.common.MessageResponse;
import com.smarttraffic.backend.security.SecurityUtils;
import com.smarttraffic.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/password")
    public MessageResponse updatePassword(@Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(SecurityUtils.requireCurrentUser(), request);
        return new MessageResponse("Cập nhật mật khẩu thành công!");
    }

    @PutMapping("/profile")
    public MessageResponse updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        userService.updateProfile(SecurityUtils.requireCurrentUser(), request);
        return new MessageResponse("Cập nhật thông tin thành công!");
    }
}
