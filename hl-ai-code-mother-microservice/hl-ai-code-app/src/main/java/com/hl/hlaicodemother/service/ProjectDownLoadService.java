package com.hl.hlaicodemother.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * 项目代码下载服务
 */
public interface ProjectDownLoadService {

    /**
     * 下载项目代码
     *
     * @param projectDirPath 项目目录路径
     * @param downloadFileName 下载文件名
     * @param response 响应
     */
    void downloadProjectAsZip(String projectDirPath, String downloadFileName, HttpServletResponse response);
}
