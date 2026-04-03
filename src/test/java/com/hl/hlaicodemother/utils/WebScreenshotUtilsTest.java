package com.hl.hlaicodemother.utils;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

@Slf4j
class WebScreenshotUtilsTest {

    @Test
    void saveWebPageScreenshot() {
        String url = "https://www.baidu.com";
        String path = WebScreenshotUtils.saveWebPageScreenshot(url);
        log.info("截图保存路径：{}", path);
        Assertions.assertNotNull(path);
    }
}