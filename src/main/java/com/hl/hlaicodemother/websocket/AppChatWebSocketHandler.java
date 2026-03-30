package com.hl.hlaicodemother.websocket;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hl.hlaicodemother.constant.UserConstant;
import com.hl.hlaicodemother.exception.BusinessException;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.manager.AppChatLockManager;
import com.hl.hlaicodemother.manager.AppChatSessionManager;
import com.hl.hlaicodemother.model.dto.appChat.AppChatCommandRequest;
import com.hl.hlaicodemother.model.dto.appChat.AppChatEvent;
import com.hl.hlaicodemother.model.entity.App;
import com.hl.hlaicodemother.model.entity.AppMember;
import com.hl.hlaicodemother.model.entity.User;
import com.hl.hlaicodemother.model.vo.AppChatConnectionVO;
import com.hl.hlaicodemother.model.vo.UserVO;
import com.hl.hlaicodemother.service.AppMemberService;
import com.hl.hlaicodemother.service.AppService;
import com.hl.hlaicodemother.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 应用协作对话 WebSocket 处理器
 */
@Component
@Slf4j
public class AppChatWebSocketHandler extends TextWebSocketHandler {

    @Resource
    private AppService appService;

    @Resource
    private AppMemberService appMemberService;

    @Resource
    private UserService userService;

    @Resource
    private AppChatLockManager appChatLockManager;

    @Resource
    private AppChatSessionManager appChatSessionManager;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        try {
            Long appId = parseAppId(session.getUri());
            User loginUser = getLoginUser(session);
            App app = appService.getById(appId);
            appService.checkAppViewAuth(app, loginUser);
            String memberRole = resolveMemberRole(app, loginUser);
            appChatSessionManager.register(appId, session);
            UserVO userVO = userService.getUserVO(loginUser);
            AppChatLockManager.ConnectionState connectionState =
                    appChatLockManager.registerConnection(appId, session.getId(), userVO, memberRole);
            sendStateSnapshot(session, appId, connectionState.getAccessMode());
            if ("editor".equals(connectionState.getAccessMode())) {
                appChatSessionManager.broadcast(appId, AppChatEvent.builder()
                        .eventType("lock_acquired")
                        .appId(appId)
                        .eventTime(LocalDateTime.now())
                        .data(appChatLockManager.getChatState(appId))
                        .build());
            }
        } catch (BusinessException e) {
            log.warn("建立应用协作 WebSocket 连接失败: {}", e.getMessage());
            session.close(CloseStatus.POLICY_VIOLATION.withReason(StrUtil.blankToDefault(e.getMessage(), "连接失败")));
        } catch (Exception e) {
            log.error("建立应用协作 WebSocket 连接异常", e);
            session.close(CloseStatus.SERVER_ERROR.withReason("应用协作连接失败"));
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        AppChatCommandRequest commandRequest = JSONUtil.toBean(message.getPayload(), AppChatCommandRequest.class);
        String action = StrUtil.blankToDefault(commandRequest.getAction(), "");
        Long appId = appChatSessionManager.getAppId(session.getId());
        if (appId == null) {
            sendErrorEvent(session, "应用协作连接不存在");
            return;
        }
        switch (action) {
            case "enter" -> handleEnter(session, appId);
            case "leave" -> handleLeave(session, appId);
            default -> sendErrorEvent(session, "不支持的协作指令");
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long appId = appChatSessionManager.getAppId(session.getId());
        boolean released = appChatLockManager.releaseIfOccupant(session.getId());
        appChatLockManager.removeConnection(session.getId());
        appChatSessionManager.unregister(session.getId());
        if (released && appId != null) {
            appChatSessionManager.broadcast(appId, AppChatEvent.builder()
                    .eventType("lock_released")
                    .appId(appId)
                    .eventTime(LocalDateTime.now())
                    .data(appChatLockManager.getChatState(appId))
                    .build());
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("应用协作 WebSocket 传输异常, sessionId: {}", session.getId(), exception);
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR.withReason("连接中断"));
        }
    }

    private void handleEnter(WebSocketSession session, Long appId) {
        boolean acquired = appChatLockManager.tryAcquire(appId, session.getId());
        sendStateSnapshot(session, appId, acquired ? "editor" : "viewer");
        if (acquired) {
            appChatSessionManager.broadcast(appId, AppChatEvent.builder()
                    .eventType("lock_acquired")
                    .appId(appId)
                    .eventTime(LocalDateTime.now())
                    .data(appChatLockManager.getChatState(appId))
                    .build());
        }
    }

    private void handleLeave(WebSocketSession session, Long appId) {
        boolean released = appChatLockManager.releaseIfOccupant(session.getId());
        sendStateSnapshot(session, appId, "viewer");
        if (released) {
            appChatSessionManager.broadcast(appId, AppChatEvent.builder()
                    .eventType("lock_released")
                    .appId(appId)
                    .eventTime(LocalDateTime.now())
                    .data(appChatLockManager.getChatState(appId))
                    .build());
        }
    }

    private void sendStateSnapshot(WebSocketSession session, Long appId, String accessMode) {
        appChatSessionManager.sendToSession(session, AppChatEvent.builder()
                .eventType("state_snapshot")
                .appId(appId)
                .eventTime(LocalDateTime.now())
                .data(AppChatConnectionVO.builder()
                        .accessMode(accessMode)
                        .state(appChatLockManager.getChatState(appId))
                        .build())
                .build());
    }

    private void sendErrorEvent(WebSocketSession session, String errorMessage) {
        appChatSessionManager.sendToSession(session, AppChatEvent.builder()
                .eventType("error")
                .eventTime(LocalDateTime.now())
                .data(Map.of("message", errorMessage))
                .build());
    }

    private Long parseAppId(URI uri) {
        if (uri == null || StrUtil.isBlank(uri.getPath())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用协作地址不合法");
        }
        String[] segments = uri.getPath().split("/");
        if (segments.length < 2) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用协作地址不合法");
        }
        String appIdStr = segments[segments.length - 2];
        if (!StrUtil.isNumeric(appIdStr)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "应用 ID 不合法");
        }
        return Long.parseLong(appIdStr);
    }

    private User getLoginUser(WebSocketSession session) {
        Object userObj = session.getAttributes().get(UserConstant.USER_LOGIN_STATE);
        User user = (User) userObj;
        if (user == null || user.getId() == null || user.getId() <= 0) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        User currentUser = userService.getById(user.getId());
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    private String resolveMemberRole(App app, User loginUser) {
        if (loginUser.getId().equals(app.getUserId())) {
            return "owner";
        }
        if (UserConstant.ADMIN_ROLE.equals(loginUser.getUserRole())) {
            return UserConstant.ADMIN_ROLE;
        }
        AppMember appMember = appMemberService.getAppMember(app.getId(), loginUser.getId());
        if (appMember == null) {
            return "";
        }
        return StrUtil.blankToDefault(appMember.getMemberRole(), "");
    }
}
