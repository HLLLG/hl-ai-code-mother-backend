package com.hl.hlaicodemother.service.impl;

import cn.hutool.core.util.ObjUtil;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.exception.ThrowUtils;
import com.hl.hlaicodemother.model.entity.App;
import com.hl.hlaicodemother.service.AppService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.hl.hlaicodemother.model.entity.AppVersion;
import com.hl.hlaicodemother.mapper.AppVersionMapper;
import com.hl.hlaicodemother.service.AppVersionService;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

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

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public int addVersion(App app, String message) {
        // 从数据库中获取当前应用的版本数量
        int versionCount = (int) this.count(new QueryWrapper().eq(AppVersion::getAppId, app.getId()));
        Long currentVersionId = app.getCurrentVersionId();
        // 构造入库对象
        AppVersion appVersion = new AppVersion();
        if (versionCount == 0 && ObjUtil.isEmpty(currentVersionId)) {
            // 如果没有版本信息，则初始化版本信息
            appVersion.setAppId(app.getId());
            appVersion.setCodeGenType(app.getCodeGenType());
            appVersion.setInitPrompt(message);
            appVersion.setCreateUserId(app.getUserId());
        } else {
            // 如果有版本信息，则基于当前版本信息创建新版本
            AppVersion currentVersion = getById(currentVersionId);
            ThrowUtils.throwIf(currentVersion == null, ErrorCode.NOT_FOUND_ERROR, "当前版本信息不存在");
            BeanUtils.copyProperties(currentVersion, appVersion);
            appVersion.setId(null);
        }
        appVersion.setUserPrompt(message);
        appVersion.setVersion(versionCount + 1);
        // 开启事务
        transactionTemplate.execute(status -> {
            // 保存新版本信息
            boolean appVersionResult = this.save(appVersion);
            ThrowUtils.throwIf(!appVersionResult, ErrorCode.OPERATION_ERROR, "初始化版本信息失败");
            Long appVersionId = appVersion.getId();
            // 将versionId回写app记录中
            App updateApp = new App();
            updateApp.setId(app.getId());
            updateApp.setCurrentVersionId(appVersionId);
            updateApp.setCurrentVersion(versionCount + 1);
            boolean appResult = appService.updateById(updateApp);
            ThrowUtils.throwIf(!appResult, ErrorCode.OPERATION_ERROR, "更新应用当前版本信息失败");
            return null;
        });
        return versionCount + 1;
    }
}
