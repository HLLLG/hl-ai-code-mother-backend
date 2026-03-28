package com.hl.hlaicodemother.service;

import com.hl.hlaicodemother.model.entity.App;
import com.mybatisflex.core.service.IService;
import com.hl.hlaicodemother.model.entity.AppVersion;

/**
 * 应用版本表 服务层。
 *
 * @author <a href="https://github.com/HLLLG">程序员HL</a>
 */
public interface AppVersionService extends IService<AppVersion> {

    /**
     * 添加版本信息
     *
     * @param app     应用信息
     * @param message 用户输入的消息
     * @return
     */
    int addVersion(App app, String message);
}
