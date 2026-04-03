package com.hl.hlaicodemother.service.impl;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.exception.ThrowUtils;
import com.hl.hlaicodemother.manager.CosManager;
import com.hl.hlaicodemother.service.ScreenshotService;
import com.hl.hlaicodemother.utils.WebScreenshotUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class ScreenshotServiceIml implements ScreenshotService {

    @Resource
    private CosManager cosManager;

    @Override
    public String generateAndUploadScreenshot(String webUrl) {
        // 校验参数
        ThrowUtils.throwIf(StrUtil.isBlank(webUrl), ErrorCode.PARAMS_ERROR, "网页URL不能为空");
        // 生成截图
        String localScreenshotPath = WebScreenshotUtils.saveWebPageScreenshot(webUrl);
        // 上传截图到云存储
        try {
            String cosUrl = uploadScreenshotToCos(localScreenshotPath);
            ThrowUtils.throwIf(StrUtil.isBlank(cosUrl), ErrorCode.OPERATION_ERROR, "上传截图失败");
            log.info("上传截图成功: {}", cosUrl);
            return cosUrl;
        } finally {
            // 清理临时文件
            FileUtil.del(localScreenshotPath);
        }
    }

    /**
     * 上传截图到云存储
     * @param localScreenshotPath
     * @return
     */
    private String uploadScreenshotToCos(String localScreenshotPath) {
        if (StrUtil.isBlank(localScreenshotPath)) {
            return null;
        }
        File file = FileUtil.file(localScreenshotPath);
        if (!file.exists()) {
            log.error("本地截图文件不存在：{}", localScreenshotPath);
            return null;
        }
        String fileName = UUID.randomUUID().toString().substring(0, 8) + "_compressed.jpg";
        String cosKey = generateScreenShotKey(fileName);
        return cosManager.uploadFile(cosKey, file);
    }

    /**
     * 生成截图的存储key
     * @param fileName
     * @return
     */
    private String generateScreenShotKey(String fileName) {
        String datePath = DateUtil.format(new Date(), "yyyy/MM/dd");
        return "/screenshots/" + datePath + "/" + fileName;
    }
}
