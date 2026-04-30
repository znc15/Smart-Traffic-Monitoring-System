package com.smarttraffic.backend.security;

import com.smarttraffic.backend.model.UserEntity;
import com.smarttraffic.backend.repository.UserRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CurrentUserResolverTest {

    @Test
    void resolve_shouldUseLatestRoleFromDatabase() {
        JwtService jwtService = mock(JwtService.class);
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUserResolver resolver = new CurrentUserResolver(jwtService, userRepository);

        when(jwtService.parseToken("token"))
                .thenReturn(Optional.of(new CurrentUser(7L, "tester", "tester@example.com", "13800000000", CurrentUser.ADMIN_ROLE_ID)));
        when(userRepository.findById(7L)).thenReturn(Optional.of(userEntity(7L, CurrentUser.USER_ROLE_ID, true)));

        Optional<CurrentUser> resolved = resolver.resolve("token");

        assertTrue(resolved.isPresent());
        assertEquals(CurrentUser.USER_ROLE_ID, resolved.get().roleId());
        assertFalse(resolved.get().isAdmin());
    }

    @Test
    void resolve_shouldRejectDisabledAccounts() {
        JwtService jwtService = mock(JwtService.class);
        UserRepository userRepository = mock(UserRepository.class);
        CurrentUserResolver resolver = new CurrentUserResolver(jwtService, userRepository);

        when(jwtService.parseToken("token"))
                .thenReturn(Optional.of(new CurrentUser(7L, "tester", "tester@example.com", "13800000000", CurrentUser.ADMIN_ROLE_ID)));
        when(userRepository.findById(7L)).thenReturn(Optional.of(userEntity(7L, CurrentUser.ADMIN_ROLE_ID, false)));

        assertTrue(resolver.resolve("token").isEmpty());
    }

    private static UserEntity userEntity(Long id, int roleId, boolean enabled) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setUsername("tester");
        user.setEmail("tester@example.com");
        user.setPhoneNumber("13800000000");
        user.setRoleId(roleId);
        user.setEnabled(enabled);
        return user;
    }
}
