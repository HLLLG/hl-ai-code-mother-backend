package com.hl.hlaicodemother.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.ZipUtil;
import com.hl.hlaicodemother.exception.BusinessException;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.exception.ThrowUtils;
import com.hl.hlaicodemother.service.ProjectDownLoadService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileFilter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Set;

@Service
@Slf4j
public class ProjectDownLoadServiceImpl implements ProjectDownLoadService {

    /**
     * 需要过滤的文件和目录名称
     */
    private static final Set<String> IGNORE_NAMES = Set.of(
            "node_modules",
            ".git",
            "dist",
            "build",
            ".DS_Store",
            ".env",
            ".mvn",
            ".idea",
            "target",
            ".vscode");

    /**
     * 需要过滤的文件扩展名
     */
    private static final Set<String> IGNORE_EXTENSIONS = Set.of(
            ".log",
            ".tmp",
            ".cache"
    );

    @Override
    public void downloadProjectAsZip(String projectRoot, String downloadFileName, HttpServletResponse response) {
        // 校验参数
        ThrowUtils.throwIf(StrUtil.isBlank(projectRoot), ErrorCode.PARAMS_ERROR, "项目路径不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(downloadFileName), ErrorCode.PARAMS_ERROR, "下载文件名不能为空");
        // 检查项目目录是否存在
        File projectDir = new File(projectRoot);
        ThrowUtils.throwIf(!projectDir.exists(), ErrorCode.NOT_FOUND_ERROR, "项目目录不存在");
        ThrowUtils.throwIf(!projectDir.isDirectory(), ErrorCode.PARAMS_ERROR, "项目路径必须是目录");
        log.info("开始打包下载项目：{} -> {}.zip", projectRoot, downloadFileName);
        // 设置响应头
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/zip");
        response.addHeader("Content-Disposition", String.format("attachment; filename=\"%s.zip\"", downloadFileName));
        // 定义文件过滤器
        FileFilter fileFilter = file -> isPathAllowed(projectDir.toPath(), file.toPath());
        // 创建 Zip 文件
        try {
            ZipUtil.zip(response.getOutputStream(), StandardCharsets.UTF_8, false, fileFilter, projectDir);
            log.info("项目打包下载成功：{}", downloadFileName);
        } catch (Exception e) {
            log.info("项目打包下载失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "项目打包下载失败");
        }
    }

    boolean isPathAllowed(Path projectRoot, Path fullPath) {
        // 获取相对路径
        Path relativize = projectRoot.relativize(fullPath);
        // 检查路径中的每一部分
        for (Path path : relativize) {
            String name = path.toString();
            // 检查是否在忽略名称列表中
            if (IGNORE_NAMES.contains(name)) {
                return false;
            }
            // 检查文件扩展名
            if (IGNORE_EXTENSIONS.stream().anyMatch(name::endsWith)) {
                return false;
            }
        }
        return true;
    }


}
