package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.config.TrafficProperties;
import com.smarttraffic.backend.dto.admin.AdminUserResponse;
import com.smarttraffic.backend.model.UserEntity;
import com.smarttraffic.backend.repository.CameraRepository;
import com.smarttraffic.backend.repository.TrafficEventRepository;
import com.smarttraffic.backend.repository.TrafficSampleRepository;
import com.smarttraffic.backend.repository.UserRepository;
import com.smarttraffic.backend.security.CurrentUser;
import com.smarttraffic.backend.security.CurrentUserAuthentication;
import com.smarttraffic.backend.service.CameraPollerService;
import com.smarttraffic.backend.service.RoadService;
import com.smarttraffic.backend.service.SystemMetricsService;
import com.smarttraffic.backend.service.TrafficService;
import com.smarttraffic.backend.service.analytics.RedisCacheService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminControllerTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateUser_shouldMapSuperuserTrueToAdminRole() {
        UserRepository userRepository = mock(UserRepository.class);
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        UserEntity user = mockUser(2L, CurrentUser.USER_ROLE_ID);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(authWithRole(1L, CurrentUser.ADMIN_ROLE_ID));

        AdminController controller = newController(userRepository, redisCacheService);

        AdminUserResponse response = controller.updateUser(2L, Map.of("is_superuser", true));

        assertEquals(CurrentUser.ADMIN_ROLE_ID, response.getRoleId());
        assertEquals(CurrentUser.ADMIN_ROLE_ID, user.getRoleId());
        verify(redisCacheService).evictUserInfo(2L);
    }

    @Test
    void updateUser_shouldMapSuperuserFalseToRegularUserRole() {
        UserRepository userRepository = mock(UserRepository.class);
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        UserEntity user = mockUser(2L, CurrentUser.ADMIN_ROLE_ID);
        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(UserEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(authWithRole(1L, CurrentUser.ADMIN_ROLE_ID));

        AdminController controller = newController(userRepository, redisCacheService);

        AdminUserResponse response = controller.updateUser(2L, Map.of("is_superuser", false));

        assertEquals(CurrentUser.USER_ROLE_ID, response.getRoleId());
        assertEquals(CurrentUser.USER_ROLE_ID, user.getRoleId());
        verify(redisCacheService).evictUserInfo(2L);
    }

    private static AdminController newController(UserRepository userRepository, RedisCacheService redisCacheService) {
        return new AdminController(
                mock(SystemMetricsService.class),
                mock(CameraRepository.class),
                userRepository,
                mock(TrafficService.class),
                mock(TrafficProperties.class),
                mock(CameraPollerService.class),
                redisCacheService,
                mock(RoadService.class),
                mock(PasswordEncoder.class),
                mock(TrafficSampleRepository.class),
                mock(TrafficEventRepository.class)
        );
    }

    private static CurrentUserAuthentication authWithRole(Long id, int roleId) {
        CurrentUser user = new CurrentUser(id, "admin", "admin@example.com", "13800000000", roleId);
        return new CurrentUserAuthentication(user, "token");
    }

    private static UserEntity mockUser(Long id, int roleId) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername("tester");
        user.setEmail("tester@example.com");
        user.setPhoneNumber("13800000000");
        user.setRoleId(roleId);
        user.setEnabled(true);
        return user;
    }
}
