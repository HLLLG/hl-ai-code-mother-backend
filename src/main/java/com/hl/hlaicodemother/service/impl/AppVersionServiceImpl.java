package com.hl.hlaicodemother.service.impl;

import cn.hutool.core.util.StrUtil;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.exception.ThrowUtils;
import com.hl.hlaicodemother.model.dto.app.AppAddRequest;
import com.hl.hlaicodemother.model.entity.App;
import com.hl.hlaicodemother.model.entity.User;
import com.hl.hlaicodemother.model.enums.CodeGenTypeEnum;
import com.hl.hlaicodemother.service.AppService;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.hl.hlaicodemother.model.entity.AppVersion;
import com.hl.hlaicodemother.mapper.AppVersionMapper;
import com.hl.hlaicodemother.service.AppVersionService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

/**
 * 应用版本表 服务层实现。
 *
 * @author <a href="https://github.com/HLLLG">程序员HL</a>
 */
@Service
public class AppVersionServiceImpl extends ServiceImpl<AppVersionMapper, AppVersion>  implements AppVersionService{

    @Resource
    @Lazy
    private AppService appService;

    @Override
    public void initVersion(AppAddRequest appAddRequest, Long appId, User user) {
        // 构造入库对象
        AppVersion appVersion = new AppVersion();
        BeanUtils.copyProperties(appAddRequest, appVersion);
        appVersion.setAppId(appId);
        if (StrUtil.isBlank(appVersion.getCodeGenType())) {
            // 默认多文件生成
            appVersion.setCodeGenType(CodeGenTypeEnum.MULTI_FILE.getValue());
        }
        appVersion.setCreateUserId(user.getId());
        boolean result = this.save(appVersion);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "初始化版本信息失败");
        Long appVersionId = appVersion.getId();
        // 将versionId回写app记录中
        App app = new App();
        app.setId(appId);
        app.setCurrentVersionId(appVersionId);
        appService.updateById(app);
    }

    @Override
    public AppVersion getLatestVersion(Long appId) {
        return null;
    }
}
