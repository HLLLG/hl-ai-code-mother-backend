package com.hl.hlaicodemother.service.impl;

import com.hl.hlaicodemother.exception.BusinessException;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.model.entity.App;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AppServiceImplTest {

    private final AppServiceImpl appService = new AppServiceImpl();

    @Test
    void validAppShouldRejectBlankInitPromptWhenAdd() {
        App app = new App();
        app.setCodeGenType("html");

        BusinessException businessException = assertThrows(BusinessException.class,
                () -> appService.validApp(app, true));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), businessException.getCode());
        assertEquals("initPrompt 不能为空", businessException.getMessage());
    }

    @Test
    void validAppShouldRejectInvalidCodeGenType() {
        App app = new App();
        app.setInitPrompt("test prompt");
        app.setCodeGenType("vue");

        BusinessException businessException = assertThrows(BusinessException.class,
                () -> appService.validApp(app, true));

        assertEquals(ErrorCode.PARAMS_ERROR.getCode(), businessException.getCode());
        assertEquals("代码生成类型不合法", businessException.getMessage());
    }


    @Test
    void validAppShouldPassForNormalAdd() {
        App app = new App();
        app.setAppName("我的应用");
        app.setInitPrompt("帮我生成一个登录页面");
        app.setCodeGenType("html");
        app.setPriority(0);

        assertDoesNotThrow(() -> appService.validApp(app, true));
    }
}

