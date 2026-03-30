package com.hl.hlaicodemother.manager.websocket;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hl.hlaicodemother.manager.websocket.model.appChat.AppChatMessageTypeEnum;
import com.hl.hlaicodemother.manager.websocket.model.appChat.AppChatRequestMessage;
import com.hl.hlaicodemother.manager.websocket.model.appChat.AppChatResponseMessage;
import com.hl.hlaicodemother.model.entity.AppMember;
import com.hl.hlaicodemother.model.entity.User;
import com.hl.hlaicodemother.model.enums.AppMemberRoleEnum;
import com.hl.hlaicodemother.service.AppMemberService;
import com.hl.hlaicodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 应用协作对话 WebSocket 处理器
 */
@Component
@Slf4j
public class AppChatWebSocketHandler extends TextWebSocketHandler {

    // 每个应用的对话状态，key：appId， value：当前正在编辑的用户ID
    private static final Map<Long, Long> appChatUsers = new ConcurrentHashMap<>();

    /**
     * 保存所有连接的会话， key：appId， value：用户会话集合
     */
    private static final Map<Long, Set<WebSocketSession>> appSessions = new ConcurrentHashMap<>();

    @Resource
    private UserService userService;

    @Resource
    private AppMemberService appMemberService;

    @Resource
    private ObjectMapper objectMapper;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 保存会话到集合中
        User user = (User) session.getAttributes().get("user");
        Long appId = (Long) session.getAttributes().get("appId");
        appSessions.putIfAbsent(appId, ConcurrentHashMap.newKeySet());
        appSessions.get(appId).add(session);

        // 构造响应
        AppChatResponseMessage appChatResponseMessage = new AppChatResponseMessage();
        appChatResponseMessage.setType(AppChatMessageTypeEnum.INFO.getValue());
        String message = String.format("%s加入会话", user.getUserName());
        appChatResponseMessage.setMessage(message);
        appChatResponseMessage.setUser(userService.getUserVO(user));
        // 广播给同一应用的用户
        broadcastToApp(appId, appChatResponseMessage);

        // 晚加入连接：若已有成员在对话中，单播其用户信息以便前端仅依赖 WS 即可同步占用状态
        Long occupantId = appChatUsers.get(appId);
        if (occupantId != null) {
            User occupant = userService.getById(occupantId);
            if (occupant != null) {
                AppChatResponseMessage occupantSync = new AppChatResponseMessage();
                occupantSync.setType(AppChatMessageTypeEnum.ENTER_CHAT.getValue());
                occupantSync.setMessage(String.format("%s正在协作中", occupant.getUserName()));
                occupantSync.setUser(userService.getUserVO(occupant));
                session.sendMessage(new TextMessage(JSONUtil.toJsonStr(occupantSync)));
            }
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        // 将消息解析为 AppChatRequestMessage
        AppChatRequestMessage appChatRequestMessage = JSONUtil.toBean(message.getPayload(), AppChatRequestMessage.class);
        String type = appChatRequestMessage.getType();
        AppChatMessageTypeEnum appChatMessageTypeEnum = AppChatMessageTypeEnum.getByValue(type);

        // 从 Session 中获取公共参数
        Map<String, Object> attributes = session.getAttributes();
        Long appId = (Long) attributes.get("appId");
        User user = (User) attributes.get("user");
        AppMember appMember = (AppMember) attributes.get("appMember");

        if (appChatMessageTypeEnum == null) {
            sendErrorMessage(session, "不支持的消息类型", user);
            return;
        }

        // 调用对应的消息处理方法
        switch (appChatMessageTypeEnum) {
            case ENTER_CHAT -> handleEnterChatMessage(session, appId, user, appMember);
            case CHAT_ACTION -> handleChatActionMessage(session, appId, user);
            case EXIT_CHAT -> handleExitChatMessage(session, appId, user);
            default -> sendErrorMessage(session, "不支持的消息类型", user);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, @NotNull CloseStatus status) throws Exception {
        Map<String, Object> attributes = session.getAttributes();
        Long appId = (Long) attributes.get("appId");
        User user = (User) attributes.get("user");
        // 移除当前用户的编辑状态
        handleExitChatMessage(session, appId, user);

        // 移除会话
        Set<WebSocketSession> sessionSet = appSessions.get(appId);
        if (sessionSet != null) {
            sessionSet.remove(session);
            if (sessionSet.isEmpty()) {
                appSessions.remove(appId);
            }
        }
        // 响应
        AppChatResponseMessage appChatResponseMessage = new AppChatResponseMessage();
        appChatResponseMessage.setType(AppChatMessageTypeEnum.INFO.getValue());
        String message = String.format("%s退出会话", user.getUserName());
        appChatResponseMessage.setMessage(message);
        appChatResponseMessage.setUser(userService.getUserVO(user));
        broadcastToApp(appId, appChatResponseMessage);
    }

    /**
     * 处理对话行为
     * @param session
     * @param appId
     * @param user
     * @throws Exception
     */
    private void handleChatActionMessage(WebSocketSession session, Long appId, User user) throws Exception {
        Long chattingUserId = appChatUsers.get(appId);
        // 确认是当前编辑者
        if (chattingUserId != null && chattingUserId.equals(user.getId())) {
            AppChatResponseMessage appChatResponseMessage = new AppChatResponseMessage();
            appChatResponseMessage.setType(AppChatMessageTypeEnum.CHAT_ACTION.getValue());
            String message = String.format("%s执行对话", user.getUserName());
            appChatResponseMessage.setMessage(message);
            appChatResponseMessage.setUser(userService.getUserVO(user));
            // 广播给除了当前用户之外的其他用户
            broadcastToApp(appId, appChatResponseMessage, session);
            return;
        }
        sendErrorMessage(session, "当前未进入对话状态，无法执行对话", user);
    }

    /**
     * 处理退出聊天状态
     * @param session
     * @param appId
     * @param user
     * @throws Exception
     */
    private void handleExitChatMessage(WebSocketSession session, Long appId, User user) throws Exception {
        Long chattingUserId = appChatUsers.get(appId);
        // 退出
        if (chattingUserId != null && chattingUserId.equals(user.getId())) {
            // 移除当前用户的对话状态
            appChatUsers.remove(appId);
            // 构造响应，发送退出聊天状态的消息通知
            AppChatResponseMessage appChatResponseMessage = new AppChatResponseMessage();
            appChatResponseMessage.setType(AppChatMessageTypeEnum.EXIT_CHAT.getValue());
            appChatResponseMessage.setMessage(String.format("%s退出聊天状态", user.getUserName()));
            appChatResponseMessage.setUser(userService.getUserVO(user));
            broadcastToApp(appId, appChatResponseMessage, session);
        }
    }

    /**
     * 处理进入对话
     * @param session
     * @param appId
     * @param user
     * @throws Exception
     */
    private void handleEnterChatMessage(WebSocketSession session, Long appId, User user,
                                        AppMember appMember) throws Exception {
        if (!canEnterChat(appMember)) {
            sendErrorMessage(session, "只有应用 owner 和编辑者可以进入对话", user);
            return;
        }
        Long chattingUserId = appChatUsers.get(appId);
        if (chattingUserId != null && chattingUserId.equals(user.getId())) {
            AppChatResponseMessage appChatResponseMessage = new AppChatResponseMessage();
            appChatResponseMessage.setType(AppChatMessageTypeEnum.ENTER_CHAT.getValue());
            appChatResponseMessage.setMessage(String.format("%s已处于聊天状态", user.getUserName()));
            appChatResponseMessage.setUser(userService.getUserVO(user));
            session.sendMessage(new TextMessage(JSONUtil.toJsonStr(appChatResponseMessage)));
            return;
        }
        if (chattingUserId != null && !chattingUserId.equals(user.getId())) {
            sendErrorMessage(session, "当前已有成员正在协作中，请稍后再试", user);
            return;
        }
        // 没有用户正在对话，则允许进入
        if (chattingUserId == null) {
            // 设置当前用户为正在对话的用户
            appChatUsers.put(appId, user.getId());
            AppChatResponseMessage appChatResponseMessage = new AppChatResponseMessage();
            appChatResponseMessage.setType(AppChatMessageTypeEnum.ENTER_CHAT.getValue());
            String message = String.format("%s进入聊天状态", user.getUserName());
            appChatResponseMessage.setMessage(message);
            appChatResponseMessage.setUser(userService.getUserVO(user));
            broadcastToApp(appId, appChatResponseMessage);
        }
    }
    

    public boolean isChatEditor(Long appId, Long userId) {
        if (appId == null || userId == null) {
            return false;
        }
        return userId.equals(appChatUsers.get(appId));
    }

    /**
     * 当前占用对话席位的用户 id（无则 null），供 HTTP 详情等与 WS 态对齐展示
     */
    public Long getChatOccupantUserId(Long appId) {
        if (appId == null) {
            return null;
        }
        return appChatUsers.get(appId);
    }

    /**
     * 新建应用时由创建者自动占用对话席位，避免首屏必须等 WebSocket ENTER_CHAT 才能发起 SSE。
     */
    public void assignCreatorAsChatEditor(Long appId, Long userId) {
        if (appId == null || appId <= 0 || userId == null || userId <= 0) {
            return;
        }
        appChatUsers.put(appId, userId);
    }

    private boolean canEnterChat(AppMember appMember) {
        if (appMember == null) {
            return false;
        }
        AppMemberRoleEnum memberRoleEnum = AppMemberRoleEnum.getEnumByValue(appMember.getMemberRole());
        return AppMemberRoleEnum.OWNER == memberRoleEnum || AppMemberRoleEnum.EDITOR == memberRoleEnum;
    }

    private void sendErrorMessage(WebSocketSession session, String errorMessage, User user) throws Exception {
        AppChatResponseMessage appChatResponseMessage = new AppChatResponseMessage();
        appChatResponseMessage.setType(AppChatMessageTypeEnum.ERROR.getValue());
        appChatResponseMessage.setMessage(errorMessage);
        appChatResponseMessage.setUser(user == null ? null : userService.getUserVO(user));
        session.sendMessage(new TextMessage(JSONUtil.toJsonStr(appChatResponseMessage)));
    }


    /**
     * 广播应用协作事件(排除指定会话)
     * @param appId
     * @param appChatResponseMessage
     * @param excludeSession
     * @throws Exception
     */
    private void broadcastToApp(Long appId, AppChatResponseMessage appChatResponseMessage,
                                WebSocketSession excludeSession) throws Exception {
        // 获取应用所有会话
        Set<WebSocketSession> sessionSet = appSessions.get(appId);
        if (CollUtil.isNotEmpty(sessionSet)) {
            String message = objectMapper.writeValueAsString(appChatResponseMessage);
            TextMessage textMessage = new TextMessage(message);
            // 遍历所有会话
            for (WebSocketSession session : sessionSet) {
                // 排除当前会话
                if (excludeSession != null && excludeSession.equals(session)) {
                    continue;
                }
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        }
    }

    /**
     * 广播应用协作事件（默认所有会话）
     */
    private void broadcastToApp(Long appId, AppChatResponseMessage appChatResponseMessage) throws Exception {
        broadcastToApp(appId, appChatResponseMessage, null);
    }

    /**
     * 向同应用下所有 WebSocket 会话广播，但排除指定用户（用于将 SSE 流镜像给围观成员，避免编辑者多连接重复收流）
     */
    public void broadcastToAppExceptUser(Long appId, Long excludeUserId, AppChatResponseMessage appChatResponseMessage) {
        if (appId == null || excludeUserId == null) {
            return;
        }
        try {
            Set<WebSocketSession> sessionSet = appSessions.get(appId);
            if (CollUtil.isEmpty(sessionSet)) {
                return;
            }
            String json = objectMapper.writeValueAsString(appChatResponseMessage);
            TextMessage textMessage = new TextMessage(json);
            for (WebSocketSession session : sessionSet) {
                User u = (User) session.getAttributes().get("user");
                if (u != null && excludeUserId.equals(u.getId())) {
                    continue;
                }
                if (session.isOpen()) {
                    session.sendMessage(textMessage);
                }
            }
        } catch (Exception e) {
            log.warn("broadcastToAppExceptUser failed, appId={}", appId, e);
        }
    }

}
