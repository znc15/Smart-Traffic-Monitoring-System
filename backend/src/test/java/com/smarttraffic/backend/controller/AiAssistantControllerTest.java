package com.smarttraffic.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smarttraffic.backend.model.AiChatConversationEntity;
import com.smarttraffic.backend.repository.AiChatConversationRepository;
import com.smarttraffic.backend.repository.AiChatMessageRepository;
import com.smarttraffic.backend.security.CurrentUser;
import com.smarttraffic.backend.security.CurrentUserAuthentication;
import com.smarttraffic.backend.service.AiToolExecutor;
import com.smarttraffic.backend.service.LlmService;
import com.smarttraffic.backend.service.TrafficService;
import com.smarttraffic.backend.service.analytics.RedisCacheService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AiAssistantControllerTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void clearConversationMessages_shouldDeleteMessagesAndEvictCache() throws Exception {
        AiChatConversationRepository conversationRepo = mock(AiChatConversationRepository.class);
        AiChatMessageRepository messageRepo = mock(AiChatMessageRepository.class);
        RedisCacheService redisCacheService = mock(RedisCacheService.class);
        AiChatConversationEntity conversation = new AiChatConversationEntity();
        conversation.setId(10L);
        conversation.setUserId(1L);
        when(conversationRepo.findById(10L)).thenReturn(Optional.of(conversation));
        SecurityContextHolder.getContext().setAuthentication(authWithRole(CurrentUser.USER_ROLE_ID));

        AiAssistantController controller = newController(conversationRepo, messageRepo, redisCacheService);

        controller.clearConversationMessages(10L);

        verify(messageRepo).deleteByConversationId(10L);
        verify(redisCacheService).evictAiMessages(10L);
    }

    @Test
    void clearConversationMessages_shouldRunInTransaction() throws Exception {
        Method method = AiAssistantController.class.getMethod("clearConversationMessages", Long.class);

        assertNotNull(method.getAnnotation(Transactional.class));
    }

    private static AiAssistantController newController(
            AiChatConversationRepository conversationRepo,
            AiChatMessageRepository messageRepo,
            RedisCacheService redisCacheService
    ) {
        return new AiAssistantController(
                mock(LlmService.class),
                mock(AiToolExecutor.class),
                conversationRepo,
                messageRepo,
                mock(TrafficService.class),
                redisCacheService,
                new ObjectMapper()
        );
    }

    private static CurrentUserAuthentication authWithRole(int roleId) {
        CurrentUser user = new CurrentUser(1L, "tester", "tester@example.com", "13800000000", roleId);
        return new CurrentUserAuthentication(user, "token");
    }
}
