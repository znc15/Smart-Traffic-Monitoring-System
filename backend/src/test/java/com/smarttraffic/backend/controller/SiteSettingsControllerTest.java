package com.smarttraffic.backend.controller;

import com.smarttraffic.backend.dto.admin.UpdateSiteSettingsRequest;
import com.smarttraffic.backend.exception.AppException;
import com.smarttraffic.backend.model.SiteSettingsEntity;
import com.smarttraffic.backend.repository.SiteSettingsRepository;
import com.smarttraffic.backend.security.CurrentUser;
import com.smarttraffic.backend.security.CurrentUserAuthentication;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SiteSettingsControllerTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void get_shouldReturnDefaultsWhenSettingsRowIsMissing() {
        SiteSettingsRepository repo = mock(SiteSettingsRepository.class);
        when(repo.findById(1L)).thenReturn(Optional.empty());

        SiteSettingsController controller = new SiteSettingsController(repo);

        SiteSettingsEntity response = controller.get();

        assertEquals("智能交通监控系统", response.getSiteName());
        assertEquals("", response.getAnnouncement());
        assertNull(response.getAmapKey());
    }

    @Test
    void update_shouldPersistTrimmedAmapKeyForAdmin() {
        SiteSettingsRepository repo = mock(SiteSettingsRepository.class);
        SiteSettingsEntity existing = new SiteSettingsEntity();
        existing.setId(1L);
        existing.setSiteName("旧站点");
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any(SiteSettingsEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(authWithRole(0));

        SiteSettingsController controller = new SiteSettingsController(repo);
        UpdateSiteSettingsRequest request = new UpdateSiteSettingsRequest();
        request.setAmapKey("  demo-amap-key  ");

        SiteSettingsEntity response = controller.update(request);

        assertEquals("demo-amap-key", response.getAmapKey());
        verify(repo).save(existing);
    }

    @Test
    void update_shouldRejectNonAdminUsers() {
        SiteSettingsRepository repo = mock(SiteSettingsRepository.class);
        SecurityContextHolder.getContext().setAuthentication(authWithRole(1));

        SiteSettingsController controller = new SiteSettingsController(repo);
        UpdateSiteSettingsRequest request = new UpdateSiteSettingsRequest();
        request.setAmapKey("demo-amap-key");

        AppException ex = assertThrows(AppException.class, () -> controller.update(request));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void update_shouldStoreNullWhenAmapKeyIsBlank() {
        SiteSettingsRepository repo = mock(SiteSettingsRepository.class);
        SiteSettingsEntity existing = new SiteSettingsEntity();
        existing.setId(1L);
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any(SiteSettingsEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(authWithRole(0));

        SiteSettingsController controller = new SiteSettingsController(repo);
        UpdateSiteSettingsRequest request = new UpdateSiteSettingsRequest();
        request.setAmapKey("   ");

        SiteSettingsEntity response = controller.update(request);

        assertNull(response.getAmapKey());
    }

    private CurrentUserAuthentication authWithRole(int roleId) {
        CurrentUser user = new CurrentUser(1L, "admin", "admin@example.com", "13800000000", roleId);
        return new CurrentUserAuthentication(user, "token");
    }
}
