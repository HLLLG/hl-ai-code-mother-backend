package com.hl.hlaicodemother.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hl.hlaicodemother.ai.AiGenerationTaskManager;
import com.hl.hlaicodemother.constant.AppConstant;
import com.hl.hlaicodemother.constant.UserConstant;
import com.hl.hlaicodemother.core.AiCodeGeneratorFacade;
import com.hl.hlaicodemother.manager.websocket.AppChatWebSocketHandler;
import com.hl.hlaicodemother.manager.websocket.model.appChat.AppChatMessageTypeEnum;
import com.hl.hlaicodemother.manager.websocket.model.appChat.AppChatResponseMessage;
import com.hl.hlaicodemother.manager.websocket.model.appChat.AppChatStreamPhaseEnum;
import com.hl.hlaicodemother.exception.BusinessException;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.exception.ThrowUtils;
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
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.spring.service.impl.ServiceImpl;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.io.File;
import java.io.Serializable;
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

    
    /**
     * 通过对话流式生成代码。
     * 校验参数和应用权限后，调用 AI 模型生成代码，并通过 WebSocket 向围观成员广播生成过程（开始、分块、完成/停止/错误）。
     * 同时将用户输入和 AI 响应保存到聊天历史。
     *
     * @param appId   应用 ID
     * @param message 用户输入的对话消息
     * @param user    当前操作用户
     * @return 返回包含生成代码片段的 Flux 流
     */
    @Override
    public Flux<String> chatToGenCode(Long appId, String message, User user) {
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
        AppChatResponseMessage startMsg = new AppChatResponseMessage();
        startMsg.setType(AppChatMessageTypeEnum.CHAT_STREAM.getValue());
        startMsg.setStreamId(streamId);
        startMsg.setStreamPhase(AppChatStreamPhaseEnum.START.getValue());
        startMsg.setStreamPayload(JSONUtil.toJsonStr(startPayload));
        startMsg.setUser(editorVo);
        appChatWebSocketHandler.broadcastToAppExceptUser(appId, user.getId(), startMsg);
        // 调用 AI 模型接口，生成代码
        Flux<String> contentFlux = aiCodeGeneratorFacade.generateAndSaveCodeStream(message, codeGenTypeEnum, app,
                taskKey);
        StringBuilder chunkBuilder = new StringBuilder();
        return contentFlux
                // 累积生成的代码块，用于后续保存到聊天历史
                .map(chunk -> {
                    chunkBuilder.append(chunk);
                    return chunk;
                })
                // 切换到弹性调度器，避免阻塞主线程
                .publishOn(Schedulers.boundedElastic())
                // 处理每个生成的代码块：构建消息并广播给围观成员
                .doOnNext(chunk -> {
                    AppChatResponseMessage chunkMsg = new AppChatResponseMessage();
                    chunkMsg.setType(AppChatMessageTypeEnum.CHAT_STREAM.getValue());
                    chunkMsg.setStreamId(streamId);
                    chunkMsg.setStreamPhase(AppChatStreamPhaseEnum.CHUNK.getValue());
                    chunkMsg.setStreamPayload(JSONUtil.toJsonStr(Map.of("d", chunk)));
                    chunkMsg.setUser(editorVo);
                    appChatWebSocketHandler.broadcastToAppExceptUser(appId, user.getId(), chunkMsg);
                })
                // 处理生成完成事件：根据是否被取消发送不同状态，并保存 AI 响应到聊天历史
                .doOnComplete(() -> {
                    AiGenerationTaskManager.TaskContext ctx = aiGenerationTaskManager.getTaskContext(taskKey);
                    boolean cancelled = ctx != null && ctx.isCancelled();
                    if (cancelled) {
                        // 任务被取消，发送停止消息
                        AppChatResponseMessage stoppedMsg = new AppChatResponseMessage();
                        stoppedMsg.setType(AppChatMessageTypeEnum.CHAT_STREAM.getValue());
                        stoppedMsg.setStreamId(streamId);
                        stoppedMsg.setStreamPhase(AppChatStreamPhaseEnum.STOPPED.getValue());
                        stoppedMsg.setStreamPayload(JSONUtil.toJsonStr(Map.of("message", "本次生成已停止。")));
                        stoppedMsg.setUser(editorVo);
                        appChatWebSocketHandler.broadcastToAppExceptUser(appId, user.getId(), stoppedMsg);
                    } else {
                        // 任务正常完成，发送完成消息并提示刷新应用
                        AppChatResponseMessage doneMsg = new AppChatResponseMessage();
                        doneMsg.setType(AppChatMessageTypeEnum.CHAT_STREAM.getValue());
                        doneMsg.setStreamId(streamId);
                        doneMsg.setStreamPhase(AppChatStreamPhaseEnum.DONE.getValue());
                        doneMsg.setStreamPayload(JSONUtil.toJsonStr(Map.of("refreshApp", true)));
                        doneMsg.setUser(editorVo);
                        appChatWebSocketHandler.broadcastToAppExceptUser(appId, user.getId(), doneMsg);
                    }
                    // 保存完整的 AI 响应到聊天历史
                    String aiResponse = chunkBuilder.toString();
                    if (StrUtil.isNotBlank(aiResponse)) {
                        chatHistoryService.addChatMessage(appId, aiResponse,
                                ChatHistoryMessageTypeEnum.AI.getValue(), user.getId());
                    }
                })
                // 处理生成过程中的异常：记录日志、广播错误消息并保存错误信息到聊天历史
                .doOnError(e -> {
                    log.error("AI 生成代码出错", e);
                    AppChatResponseMessage errMsg = new AppChatResponseMessage();
                    errMsg.setType(AppChatMessageTypeEnum.CHAT_STREAM.getValue());
                    errMsg.setStreamId(streamId);
                    errMsg.setStreamPhase(AppChatStreamPhaseEnum.ERROR.getValue());
                    errMsg.setStreamPayload(JSONUtil.toJsonStr(
                            Map.of("message", "生成失败：" + (e.getMessage() == null ? "未知错误" : e.getMessage()))));
                    errMsg.setUser(editorVo);
                    appChatWebSocketHandler.broadcastToAppExceptUser(appId, user.getId(), errMsg);
                    String errorMessage = "AI 生成代码出错：" + e.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage,
                            ChatHistoryMessageTypeEnum.AI.getValue(), user.getId());
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
        String sourceDirName = codeGenType + "_" + appId + "_v" + app.getCurrentVersion();
        String sourceDirPath = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + sourceDirName;
        File sourceDir = new File(sourceDirPath);
        if (!sourceDir.exists() || !sourceDir.isDirectory()) {
            // 不存在则提示先生成代码
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用生成目录不存在，请先生成代码");
        }
        // 部署应用
        String deployDirPath =
                AppConstant.CODE_DEPLOY_ROOT_DIR + File.separator + deployKey + "_v" + app.getCurrentVersion();
        FileUtil.copyContent(sourceDir, new File(deployDirPath), true);
        // 更新应用的deployKey和部署时间
        App updateApp = new App();
        updateApp.setDeployKey(deployKey);
        updateApp.setDeployedTime(LocalDateTime.now());
        updateApp.setId(appId);
        boolean updateResult = this.updateById(updateApp);
        ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用部署信息失败");
        // 返回部署访问地址
        return String.format("%s/%s/", AppConstant.CODE_DEPLOY_HOST, deployKey + "_v" + app.getCurrentVersion());
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
        if (!AppMemberRoleEnum.OWNER.getValue().equals(memberRole)
                && !AppMemberRoleEnum.EDITOR.getValue().equals(memberRole)) {
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

    private String buildGenerationTaskKey(Long appId, Long userId) {
        return userId + "_" + appId;
    }

    private void validateChatEditor(Long appId, User user) {
        if (!appChatWebSocketHandler.isChatEditor(appId, user.getId())) {
            throw new BusinessException(ErrorCode.NOT_AUTH_ERROR, "当前未进入对话状态，无法执行该操作");
        }
    }

}
