package com.hl.hlaicodemother.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hl.hlaicodemother.ai.AiCodeGenTypeRoutingService;
import com.hl.hlaicodemother.ai.AiCodeGenTypeRoutingServiceFactory;
import com.hl.hlaicodemother.ai.AiGenerationTaskManager;
import com.hl.hlaicodemother.bizmq.ScreenshotMessageProducer;
import com.hl.hlaicodemother.bizmq.model.ScreenshotTaskMessage;
import com.hl.hlaicodemother.constant.AppConstant;
import com.hl.hlaicodemother.constant.UserConstant;
import com.hl.hlaicodemother.core.AiCodeGeneratorFacade;
import com.hl.hlaicodemother.core.builder.VueProjectBuilder;
import com.hl.hlaicodemother.core.handler.StreamHandlerExecutor;
import com.hl.hlaicodemother.manager.cache.CacheAsideTemplate;
import com.hl.hlaicodemother.manager.cache.HotKeyCacheTemplate;
import com.hl.hlaicodemother.manager.cache.MultiLevelCacheTemplate;
import com.hl.hlaicodemother.monitor.MonitorContext;
import com.hl.hlaicodemother.monitor.MonitorContextHolder;
import com.hl.hlaicodemother.utils.CacheKeyUtils;
import com.hl.hlaicodemother.exception.BusinessException;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.exception.ThrowUtils;
import com.hl.hlaicodemother.manager.websocket.AppChatWebSocketHandler;
import com.hl.hlaicodemother.manager.websocket.model.appChat.AppChatStreamPhaseEnum;
import com.hl.hlaicodemother.mapper.AppMapper;
import com.hl.hlaicodemother.model.dto.app.AppAddRequest;
import com.hl.hlaicodemother.model.dto.app.AppQueryRequest;
import com.hl.hlaicodemother.model.entity.App;
import com.hl.hlaicodemother.model.entity.AppMember;
import com.hl.hlaicodemother.model.entity.User;
import com.hl.hlaicodemother.model.enums.AppMemberRoleEnum;
import com.hl.hlaicodemother.model.enums.AppMemberStatusEnum;
import com.hl.hlaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.hl.hlaicodemother.model.enums.CodeGenTypeEnum;
import com.hl.hlaicodemother.model.vo.AppVO;
import com.hl.hlaicodemother.model.vo.UserVO;
import com.hl.hlaicodemother.service.AppMemberService;
import com.hl.hlaicodemother.service.AppService;
import com.hl.hlaicodemother.service.UserService;
import cn.hutool.core.lang.TypeReference;
import com.mybatisflex.core.paginate.Page;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.io.File;
import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 应用 服务层实现。
 *
 * @author <a href="https://github.com/HLLLG">程序员HL</a>
 */
@Service
@Slf4j
public class AppServiceImpl extends ServiceImpl<AppMapper, App> implements AppService {

    @Resource
    private UserService userService;

    @Resource
    @Lazy
    private AiCodeGeneratorFacade aiCodeGeneratorFacade;

    @Resource
    private AiGenerationTaskManager aiGenerationTaskManager;

    @Resource
    private ChatHistoryServiceImpl chatHistoryService;

    @Resource
    private AppMemberService appMemberService;

    @Resource
    private AppChatWebSocketHandler appChatWebSocketHandler;

    @Resource
    private StreamHandlerExecutor streamHandlerExecutor;

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private ScreenshotMessageProducer screenshotMessageProducer;

    @Resource
    private AiCodeGenTypeRoutingServiceFactory aiCodeGenTypeRoutingServiceFactory;

    @Resource
    private MultiLevelCacheTemplate multiLevelCacheTemplate;

    @Resource
    private HotKeyCacheTemplate hotKeyCacheTemplate;

    /**
     * 应用详情缓存 key 前缀（JD-HotKey）。
     */
    private static final String APP_DETAIL_CACHE_PREFIX = "app_detail:";

    /**
     * 精选应用分页缓存 key 前缀。建议与其它业务独立，便于按前缀做批量失效。
     */
    private static final String GOOD_APP_PAGE_CACHE_PREFIX = "good_app_page:";

    /**
     * 精选应用分页缓存 TTL。
     */
    private static final Duration GOOD_APP_PAGE_CACHE_TTL = Duration.ofMinutes(5);

    /**
     * 只缓存前 N 页：深翻页属于长尾，命中率低，缓存收益不高反而污染 Redis。
     */
    private static final int GOOD_APP_PAGE_CACHE_MAX_PAGE = 10;

    /**
     * 延时双删的延迟时间。略大于一次"读 DB + 回写"耗时即可。
     */
    private static final Duration GOOD_APP_PAGE_CACHE_DOUBLE_DELETE_DELAY = Duration.ofMillis(500);


    /**
     * 通过对话流式生成代码。
     * 校验参数和应用权限后，调用 AI 模型生成代码，并通过 WebSocket 向围观成员广播生成过程（开始、分块、完成/停止/错误）。
     * 同时将用户输入和 AI 响应保存到聊天历史。
     *
     * @param appId   应用 ID
     * @param message 用户输入的对话消息
     * @param isAdd
     * @param user    当前操作用户
     * @return 返回包含生成代码片段的 Flux 流
     */
    @Override
    public Flux<String> chatToGenCode(Long appId, String message, Boolean isAdd, User user) {
        // 校验参数
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不合法");
        ThrowUtils.throwIf(StrUtil.isBlank(message), ErrorCode.PARAMS_ERROR, "用户输入不能为空");
        // 校验应用存在
        App app = getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        // 仅编辑成员且当前进入对话的用户可以发起生成
        checkAppEditAuth(app, user);
        validateChatEditor(appId, user);
        // 获取应用的代码生成类型
        String codeGenType = app.getCodeGenType();
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        ThrowUtils.throwIf(codeGenTypeEnum == null, ErrorCode.PARAMS_ERROR, "应用的代码生成类型不合法");
        // 保存用户输入的对话消息
        chatHistoryService.addChatMessage(appId, message, ChatHistoryMessageTypeEnum.USER.getValue(), user.getId());
        String taskKey = buildGenerationTaskKey(appId, user.getId());
        String streamId = UUID.randomUUID().toString();
        UserVO editorVo = userService.getUserVO(user);
        // 围观成员：新一轮对话开始
        Map<String, Object> startPayload = new HashMap<>();
        startPayload.put("userMessage", message);
        startPayload.put("user", editorVo);
        appChatWebSocketHandler.broadcastToApp(appId, user, streamId, AppChatStreamPhaseEnum.START.getValue(),
                JSONUtil.toJsonStr(startPayload), editorVo);
        // 设置监控上下文
        MonitorContextHolder.set(MonitorContext.builder()
                .appId(appId.toString())
                .userId(user.getId().toString())
                .build());
        // 调用 AI 模型接口，生成代码
        Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, app,
                taskKey, isAdd);
        return streamHandlerExecutor.doExecute(codeStream, appId, streamId, user, taskKey, editorVo,
                appChatWebSocketHandler, chatHistoryService, aiGenerationTaskManager, codeGenTypeEnum)
                .doFinally(signalType -> {
                    // 移除监控上下文
                    MonitorContextHolder.remove();
                });
    }

    @Override
    public boolean stopChatToGenCode(Long appId, User user) {
        ThrowUtils.throwIf(appId == null || appId <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不合法");
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        App app = getById(appId);
        ThrowUtils.throwIf(app == null, ErrorCode.NOT_FOUND_ERROR, "应用不存在");
        checkAppEditAuth(app, user);
        validateChatEditor(appId, user);
        return aiGenerationTaskManager.cancelTask(buildGenerationTaskKey(appId, user.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addApp(AppAddRequest appAddRequest, User loginUser) {
        String initPrompt = appAddRequest.getInitPrompt();
        ThrowUtils.throwIf(StrUtil.isBlank(initPrompt), ErrorCode.PARAMS_ERROR, "初始化提示不能为空");
        // 构造入库对象
        App app = new App();
        BeanUtils.copyProperties(appAddRequest, app);
        // 应用名称暂时设置为initPrompt的前12个字符，后续可以修改为用户输入
        if (StrUtil.isBlank(app.getAppName())) {
            app.setAppName(StrUtil.sub(appAddRequest.getInitPrompt(), 0, 12));
        }
        // 使用 Ai 智能选择代码生成类型（多例模式）
        AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService = aiCodeGenTypeRoutingServiceFactory.aiCodeGenTypeRoutingService();
        CodeGenTypeEnum codeGenTypeEnum = aiCodeGenTypeRoutingService.routeCodeGenType(initPrompt);
        app.setCodeGenType(codeGenTypeEnum.getValue());
        app.setCurrentVersion(1);
        app.setPriority(AppConstant.DEFAULT_APP_PRIORITY);
        app.setUserId(loginUser.getId());
        // 校验应用是否合法
        this.validApp(app, true);
        boolean result = this.save(app);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "应用创建失败");
        boolean addOwnerResult = appMemberService.addOwnerMember(app.getId(), loginUser.getId());
        ThrowUtils.throwIf(!addOwnerResult, ErrorCode.OPERATION_ERROR, "初始化应用 owner 失败");
        appChatWebSocketHandler.assignCreatorAsChatEditor(app.getId(), loginUser.getId());
        return app.getId();
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
        String sourceDirName = codeGenType + "_" + appId + "/v" + app.getCurrentVersion();
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            // 不存在则提示先生成代码
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用生成目录不存在，请先生成代码");
        }
        // vue 项目特殊处理：执行构建
        CodeGenTypeEnum codeGenTypeEnum = CodeGenTypeEnum.getEnumByValue(codeGenType);
        if (codeGenTypeEnum == CodeGenTypeEnum.VUE_PROJECT) {
            // 构建
            boolean buildResult = vueProjectBuilder.buildVueProject(sourceDirPath);
            ThrowUtils.throwIf(!buildResult, ErrorCode.SYSTEM_ERROR, "Vue 项目构建失败，请检查代码和依赖");
            // 检查 dist 目录是否存在
            File distDir = new File(sourceDir, "dist");
            ThrowUtils.throwIf(!distDir.exists() || !distDir.isDirectory(), ErrorCode.PARAMS_ERROR,
                    "应用生成目录不存在 dist " + "目录");
            // 将dist 目录复制到 deploy 目录
            sourceDir = distDir;
            log.info("Vue 项目构建成功, 将部署 dis 目录：{}", distDir.getAbsolutePath());
        }
        // 部署应用
        String deployDirPath =
                AppConstant.CODE_DEPLOY_ROOT_DIR + "/" + deployKey + "/v" + app.getCurrentVersion();
        FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        // 更新应用的deployKey和部署时间
        App updateApp = new App();
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        updateApp.setId(appId);
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 返回部署访问地址
        String deployUrl = String.format("%s/%s/v%s/", AppConstant.CODE_DEPLOY_HOST, deployKey,
                app.getCurrentVersion());
        ;
        generateAppScreenshotAsync(appId, deployUrl);
        return deployUrl;
    }

    @Override
    public void generateAppScreenshotAsync(Long appId, String appDeployUrl) {
        screenshotMessageProducer.sendScreenshotTask(new ScreenshotTaskMessage(appId, appDeployUrl));
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
    public AppVO getAppVOByIdCacheable(Long id) {
        ThrowUtils.throwIf(id == null || id <= 0, ErrorCode.PARAMS_ERROR, "应用 ID 不合法");
        String cacheKey = buildAppDetailCacheKey(id);
        AppVO cachedAppVO = hotKeyCacheTemplate.getOrLoad(cacheKey, AppVO.class, () -> {
            App app = getById(id);
            if (app == null) {
                return null;
            }
            return getAppVO(app);
        });
        return copyCacheSafeAppVO(cachedAppVO);
    }

    @Override
    public void invalidateAppDetailCache(Long appId) {
        if (appId == null || appId <= 0) {
            return;
        }
        hotKeyCacheTemplate.evict(buildAppDetailCacheKey(appId));
    }

    @Override
    public boolean removeById(Serializable id) {
        if (id == null) {
            return false;
        }
        // 删除应用的同时，删除相关的对话历史
        boolean result = false;
        try {
            result = super.removeById(id);
            if (result) {
                chatHistoryService.remove(new QueryWrapper().eq("appId", id));
                Long appId = parseAppId(id);
                invalidateAppDetailCache(appId);
                // 写后失效：精选列表可能受影响，按前缀延时双删
                invalidateGoodAppPageCache();
            }
        } catch (Exception e) {
            // 删除过程中发生异常，记录日志但不抛出，以免影响用户体验
            log.error("删除应用失败，id: " + id, e);
        }
        return result;
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
    public void checkAppViewAuth(App app, User user) {
        if (app == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用不存在");
        }
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        if (user.getId().equals(app.getUserId()) || UserConstant.ADMIN_ROLE.equals(user.getUserRole())) {
            return;
        }
        AppMember appMember = appMemberService.getAppMember(app.getId(), user.getId());
        if (appMember == null || !AppMemberStatusEnum.ACTIVE.getValue().equals(appMember.getMemberStatus())) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "无权查看该应用");
        }
    }

    @Override
    public void checkAppEditAuth(App app, User user) {
        if (app == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用不存在");
        }
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_LOGIN_ERROR);
        if (user.getId().equals(app.getUserId()) || UserConstant.ADMIN_ROLE.equals(user.getUserRole())) {
            return;
        }
        AppMember appMember = appMemberService.getAppMember(app.getId(), user.getId());
        if (appMember == null || !AppMemberStatusEnum.ACTIVE.getValue().equals(appMember.getMemberStatus())) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "无权编辑该应用");
        }
        String memberRole = appMember.getMemberRole();
        if (!AppMemberRoleEnum.OWNER.getValue().equals(memberRole) && !AppMemberRoleEnum.EDITOR.getValue().equals(memberRole)) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "当前成员仅支持只读查看");
        }
    }

    @Override
    public QueryWrapper getQueryWrapper(AppQueryRequest appQueryRequest) {
        if (appQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "查询请求不能为空");
        }
        return QueryWrapper.create().eq("id", appQueryRequest.getId()).like("appName", appQueryRequest.getAppName()).like("cover", appQueryRequest.getCover()).like("initPrompt", appQueryRequest.getInitPrompt()).eq("codeGenType", appQueryRequest.getCodeGenType()).eq("deployKey", appQueryRequest.getDeployKey()).eq("priority", appQueryRequest.getPriority()).eq("userId", appQueryRequest.getUserId()).orderBy(appQueryRequest.getSortField(), "ascend".equals(appQueryRequest.getSortOrder()));
    }

    @Override
    public Page<AppVO> listGoodAppVOByPage(AppQueryRequest appQueryRequest) {
        ThrowUtils.throwIf(appQueryRequest == null, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(appQueryRequest.getPageSize() > 20, ErrorCode.PARAMS_ERROR, "每页最多20条");
        appQueryRequest.setPriority(AppConstant.GOOD_APP_PRIORITY);

        // 冷数据（深翻页）不走缓存，直接查库
        if (appQueryRequest.getPageNum() == null || appQueryRequest.getPageNum() > GOOD_APP_PAGE_CACHE_MAX_PAGE) {
            return queryGoodAppVOByPageFromDb(appQueryRequest);
        }

        // 多级缓存：L1 Caffeine + 进程内单飞 + L2 Redis + Redisson 分布式锁防击穿
        String cacheKey = CacheKeyUtils.generateKeyWithPrefix(GOOD_APP_PAGE_CACHE_PREFIX, appQueryRequest);
        return multiLevelCacheTemplate.getOrLoad(
                cacheKey,
                GOOD_APP_PAGE_CACHE_TTL,
                new TypeReference<Page<AppVO>>() {}.getType(),
                () -> queryGoodAppVOByPageFromDb(appQueryRequest)
        );
    }

    /**
     * 失效精选应用分页缓存：按前缀 + 延时双删，
     * 同时通过 RTopic 广播让<b>所有节点</b>同步删 L1。
     * <p>
     * <b>调用时机</b>：管理员修改/删除应用、调整 priority 等会影响列表结果的写操作，
     * 应在<b>事务提交后</b>触发，避免出现"缓存被删 → 别的读请求拿到旧 DB 值并回写"的脏数据窗口。
     */
    @Override
    public void invalidateGoodAppPageCache() {
        multiLevelCacheTemplate.delayedEvictByPattern(
                GOOD_APP_PAGE_CACHE_PREFIX + "*",
                GOOD_APP_PAGE_CACHE_DOUBLE_DELETE_DELAY
        );
    }

    /**
     * 真正的 DB 查询逻辑，被缓存层包裹。
     * <p>
     * 语义约定：<b>库里无结果时返回"空 Page"而非 null</b>，由模板根据业务需要判定是否写哨兵。
     * 这里选择返回 totalRow=0 的 Page，外层模板仍会把它当作"非 null 的真实结果"缓存，
     * 对于本场景（分页列表）这通常是期望行为——列表页常见结果就是"空列表"。
     */
    private Page<AppVO> queryGoodAppVOByPageFromDb(AppQueryRequest appQueryRequest) {
        Page<App> appPage = this.page(
                new Page<>(appQueryRequest.getPageNum(), appQueryRequest.getPageSize()),
                getQueryWrapper(appQueryRequest)
        );
        Page<AppVO> appVOPage = new Page<>(appPage.getPageNumber(), appPage.getPageSize(), appPage.getTotalRow());
        appVOPage.setRecords(getAppVOList(appPage.getRecords()));
        return appVOPage;
    }

    @Override
    public void incrementDownloadCount(App app) {
        ThrowUtils.throwIf(app == null, ErrorCode.PARAMS_ERROR, "应用不存在");
        App updateApp = new App();
        updateApp.setId(app.getId());
        updateApp.setDownloadCount(app.getDownloadCount() + 1);
        boolean updated = this.updateById(updateApp);
        ThrowUtils.throwIf(!updated, ErrorCode.SYSTEM_ERROR, "更新下载次数失败");
    }

    /**
     * 构建生成任务 key
     *
     * @param appId  应用 id
     * @param userId 用户 id
     * @return 生成任务 key
     */
    private String buildGenerationTaskKey(Long appId, Long userId) {
        return userId + "_" + appId;
    }

    /**
     * 校验当前用户是否是当前应用的聊天编辑者
     *
     * @param appId 应用 id
     * @param user  当前用户
     */
    private void validateChatEditor(Long appId, User user) {
        if (!appChatWebSocketHandler.isChatEditor(appId, user.getId())) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "当前未进入对话状态，无法执行该操作");
        }
    }

    private String buildAppDetailCacheKey(Long appId) {
        return APP_DETAIL_CACHE_PREFIX + appId;
    }

    /**
     * 返回可变字段隔离后的对象，避免用户态字段污染 HotKey 缓存值。
     */
    private AppVO copyCacheSafeAppVO(AppVO source) {
        if (source == null) {
            return null;
        }
        AppVO target = new AppVO();
        BeanUtils.copyProperties(source, target);
        target.setMyMemberRole(null);
        target.setMyMemberStatus(null);
        target.setChatOccupantUser(null);
        return target;
    }

    private Long parseAppId(Serializable id) {
        if (id == null) {
            return null;
        }
        if (id instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(id));
        } catch (NumberFormatException e) {
            log.warn("Failed to parse appId for cache eviction, id={}", id);
            return null;
        }
    }

}
