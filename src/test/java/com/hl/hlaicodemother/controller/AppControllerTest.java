package com.hl.hlaicodemother.controller;

import com.hl.hlaicodemother.common.BaseResponse;
import com.hl.hlaicodemother.model.dto.app.AppAddRequest;
import com.hl.hlaicodemother.model.entity.User;
import com.hl.hlaicodemother.service.AppService;
import com.hl.hlaicodemother.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppControllerTest {

    private AppController appController;

    @Mock
    private AppService appService;

    @Mock
    private UserService userService;

    @BeforeEach
    void setUp() {
        appController = new AppController();
        ReflectionTestUtils.setField(appController, "appService", appService);
        ReflectionTestUtils.setField(appController, "userService", userService);
    }

    @Test
    void addAppShouldDelegateToService() {
        AppAddRequest appAddRequest = new AppAddRequest();
        appAddRequest.setInitPrompt("帮我生成一个首页");
        HttpServletRequest request = new MockHttpServletRequest();
        User loginUser = new User();
        loginUser.setId(1L);
        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(appService.addApp(appAddRequest, loginUser)).thenReturn(1001L);

        BaseResponse<Long> response = appController.addApp(appAddRequest, request);

        assertEquals(1001L, response.getData());
        verify(appService).addApp(appAddRequest, loginUser);
    }

    @Test
    void stopChatToGenCodeShouldDelegateToService() {
        HttpServletRequest request = new MockHttpServletRequest();
        User loginUser = new User();
        loginUser.setId(1L);
        when(userService.getLoginUser(request)).thenReturn(loginUser);
        when(appService.stopChatToGenCode(1L, loginUser)).thenReturn(true);

        BaseResponse<Boolean> response = appController.stopChatToGenCode(1L, request);

        assertEquals(true, response.getData());
        verify(appService).stopChatToGenCode(1L, loginUser);
    }
}
