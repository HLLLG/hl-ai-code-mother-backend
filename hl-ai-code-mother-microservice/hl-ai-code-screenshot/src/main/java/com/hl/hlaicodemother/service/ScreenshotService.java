package com.hl.hlaicodemother.service;

/**
 * 截图服务
 */
public interface ScreenshotService {

    /**
     * 生成截图并上传
     *
     * @param webUrl 网页URL
     * @return 截图URL
     */
    String generateAndUploadScreenshot(String webUrl);
}
