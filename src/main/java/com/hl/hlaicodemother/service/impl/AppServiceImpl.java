package com.hl.hlaicodemother.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.hl.hlaicodemother.constant.AppConstant;
import com.hl.hlaicodemother.constant.UserConstant;
import com.hl.hlaicodemother.core.AiCodeGeneratorFacade;
import com.hl.hlaicodemother.exception.BusinessException;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.exception.ThrowUtils;
import com.hl.hlaicodemother.mapper.AppMapper;
import com.hl.hlaicodemother.model.dto.app.AppAddRequest;
import com.hl.hlaicodemother.model.dto.app.AppQueryRequest;
import com.hl.hlaicodemother.model.entity.App;
import com.hl.hlaicodemother.model.entity.User;
import com.hl.hlaicodemother.model.enums.CodeGenTypeEnum;
import com.hl.hlaicodemother.model.vo.AppVO;
import com.hl.hlaicodemother.model.vo.UserVO;
import com.hl.hlaicodemother.service.AppService;
import com.hl.hlaicodemother.service.AppVersionService;
import com.hl.hlaicodemother.service.UserService;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/HLLLG">程序员HL</a>
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private AppVersionService appVersionService;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User user) {
        // 校验参数
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不合法");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户输入不能为空");
        // 校验应用存在
        App app = getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 校验应用归属
        checkAppOwner(app, user);
        // 获取应用的代码生成类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR, "应用的代码生成类型不合法");
        // 调用 AI 模型接口，生成代码
        return aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, app);
    }

    @Override
    public Long addApp(AppAddRequest appAddRequest, User loginUser) {
        // 构造入库对象
        App app = new App();
        BeanUtils.copyProperties(appAddRequest, app);
        // 应用名称暂时设置为initPrompt的前12个字符，后续可以修改为用户输入
        if (StrUtil.isBlank(app.getAppName())) {
            app.setAppName(StrUtil.sub(appAddRequest.getInitPrompt(), 0, 12));
        }
        if (StrUtil.isBlank(app.getCodeGenType())) {
            // 默认多文件生成
            app.setCodeGenType(CodeGenTypeEnum.MULTI_FILE.getValue());
        }
        app.setCurrentVersion(1);
        app.setPriority(AppConstant.DEFAULT_APP_PRIORITY);
        app.setUserId(loginUser.getId());
        // 校验应用是否合法
        this.validApp(app, true);
        // 开启事务
        return transactionTemplate.execute(status -> {
            boolean result = this.save(app);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "应用创建失败");
            // 初始化版本信息
            appVersionService.initVersion(appAddRequest, app.getId(), loginUser);
            return app.getId();
        });
    }

    @Override
    public String deployApp(Long appId, User user) {
        // 校验参数
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不合法");
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        // 检查应用是否存在, 且只有本人可以部署应用
        App app = getById(appId);
        checkAppOwner(app, user);
        // 检查生成的deployKey是否已存在，避免重复
        String deployKey = app.getDeployKey();
        if (StrUtil.isBlank(deployKey)) {
            // 生成deployKey，六位（数字+字母）
            deployKey = RandomUtil.randomString(6);
        }
        // 获取应用生成类型
        String codeGenType = app.getCodeGenType();
        // 检查应用生成目录是否存在
        String sourceDirName = codeGenType + "_" + appId;
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() ||  !sourceDir.isDirectory()) {
            // 不存在则提示先生成代码
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用生成目录不存在，请先生成代码");
        }
        // 部署应用
        String deployDirPath = AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey;
        FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        // 更新应用的deployKey和部署时间
        App updateApp = new App();
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        updateApp.setId(appId);
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 返回部署访问地址
        return String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey);
    }

    @Override
    public void validApp(App app, boolean add) {
        if (app == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用参数不能为空");
        }
        String appName = app.getAppName();
        String cover = app.getCover();
        String initPrompt = app.getInitPrompt();
        String codeGenType = app.getCodeGenType();
        Integer priority = app.getPriority();
        if (add && StrUtil.isBlank(initPrompt)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "initPrompt 不能为空");
        }
        if (StrUtil.isNotBlank(appName) && appName.length() > 32) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用名称过长");
        }
        if (appName != null && StrUtil.isBlank(appName)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用名称不能为空");
        }
        if (StrUtil.isNotBlank(cover) && cover.length() > 512) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用封面地址过长");
        }
        if (StrUtil.isNotBlank(codeGenType) && CodeGenTypeEnum.getEnumByValue(codeGenType) == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成类型不合法");
        }
        if (priority != null && priority < 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "优先级不能小于 0");
        }
    }

    @Override
    public AppVO getAppVO(App app) {
        if (app == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用不能为空");
        }
        AppVO appVO = new AppVO();
        BeanUtils.copyProperties(app, appVO);
        // 关联查询用户信息
        Long userId = app.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            if (user != null) {
                appVO.setUser(userService.getUserVO(user));
            }
        }
        return appVO;
    }

    @Override
    public List<AppVO> getAppVOList(List<App> appList) {
        if (CollUtil.isEmpty(appList)) {
            return new ArrayList<>();
        }
        // 关联查询用户信息，避免 N+1 查询
        Set<Long> userIdSet = appList.stream().map(App::getUserId).collect(Collectors.toSet());
        List<User> userList = userService.listByIds(userIdSet);
        // 构建用户 id 到用户 VO 的映射
        Map<Long, UserVO> userVOMap = userList.stream().collect(Collectors.toMap(User::getId, userService::getUserVO));
        // 填充用户信息到应用 VO
        return appList.stream().map(app -> {
            AppVO appVO = getAppVO(app);
            UserVO userVO = userVOMap.get(app.getUserId());
            appVO.setUser(userVO);
            return appVO;
        }).collect(Collectors.toList());
    }

    @Override
    public void checkAppOwner(App app, User user) {
        if (app == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用不存在");
        }
        // 仅本人或者管理员可操作
        if (!user.getId().equals(app.getUserId()) && !UserConstant.ADMIN_ROLE.equals(user.getUserRole())) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "无权操作该应用");
        }
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询请求不能为空");
        }
        return QueryWrapper.create().eq("id", appQueryRequest.getId()).like("appName", appQueryRequest.getAppName()).like("cover", appQueryRequest.getCover()).like("initPrompt", appQueryRequest.getInitPrompt()).eq("codeGenType", appQueryRequest.getCodeGenType()).eq("deployKey", appQueryRequest.getDeployKey()).eq("priority", appQueryRequest.getPriority()).eq("userId", appQueryRequest.getUserId()).orderBy(appQueryRequest.getSortField(), "ascend".equals(appQueryRequest.getSortOrder()));
    }


}
