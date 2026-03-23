package com.hl.hlaicodemother.controller;

import com.hl.hlaicodemother.common.BaseResponse;
import com.hl.hlaicodemother.exception.BusinessException;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.model.dto.app.AppAddRequest;
import com.hl.hlaicodemother.model.dto.app.AppAdminUpdateRequest;
import com.hl.hlaicodemother.model.entity.App;
import com.hl.hlaicodemother.model.entity.User;
import com.hl.hlaicodemother.service.AppService;
import com.hl.hlaicodemother.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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
    void addAppShouldFillDefaultFields() {
        AppAddRequest appAddRequest = new AppAddRequest();
        appAddRequest.setInitPrompt("帮我生成一个首页");
        HttpServletRequest request = new MockHttpServletRequest();
        User loginUser = new User();
        loginUser.setId(1L);
        when(userService.getLoginUser(request)).thenReturn(loginUser);
        doNothing().when(appService).validApp(any(App.class), eq(true));
        when(appService.save(any(App.class))).thenAnswer(invocation -> {
            App app = invocation.getArgument(0);
            app.setId(1001L);
            return true;
        });

        BaseResponse<Long> response = appController.addApp(appAddRequest, request);

        ArgumentCaptor<App> appArgumentCaptor = ArgumentCaptor.forClass(App.class);
        verify(appService).save(appArgumentCaptor.capture());
        App savedApp = appArgumentCaptor.getValue();
        assertEquals("未命名应用", savedApp.getAppName());
        assertEquals("html", savedApp.getCodeGenType());
        assertEquals(0, savedApp.getPriority());
        assertEquals(1L, savedApp.getUserId());
        assertEquals("帮我生成一个首页", savedApp.getInitPrompt());
        assertEquals(1001L, response.getData());
    }

}

