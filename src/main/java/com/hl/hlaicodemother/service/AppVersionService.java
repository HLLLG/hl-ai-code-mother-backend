package com.hl.hlaicodemother.service;

import com.hl.hlaicodemother.model.dto.app.AppAddRequest;
import com.hl.hlaicodemother.model.entity.User;
import com.mybatisflex.core.service.IService;
import com.hl.hlaicodemother.model.entity.AppVersion;

/**
 * 应用版本表 服务层。
 *
 * @author <a href="https://github.com/HLLLG">程序员HL</a>
 */
public interface AppVersionService extends IService<AppVersion> {

    /**
     * 初始化版本信息
     *
     * @param appAddRequest
     * @param appId
     */
    void initVersion(AppAddRequest appAddRequest, Long appId, User user);
}
