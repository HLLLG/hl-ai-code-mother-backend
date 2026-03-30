package com.hl.hlaicodemother.manager;

import com.hl.hlaicodemother.model.vo.AppChatStateVO;
import com.hl.hlaicodemother.model.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 应用协作对话锁管理器
 */
@Component
public class AppChatLockManager {

    private final Map<Long, Object> appMutexMap = new ConcurrentHashMap<>();

    private final Map<Long, ChatLockState> appLockMap = new ConcurrentHashMap<>();

    private final Map<String, ConnectionState> connectionStateMap = new ConcurrentHashMap<>();

    public ConnectionState registerConnection(Long appId, String sessionId, UserVO user, String memberRole) {
        ConnectionState connectionState = ConnectionState.builder()
                .appId(appId)
                .sessionId(sessionId)
                .userId(user.getId())
                .userName(user.getUserName())
                .userAvatar(user.getUserAvatar())
                .memberRole(memberRole)
                .accessMode("viewer")
                .build();
        connectionStateMap.put(sessionId, connectionState);
        tryAcquire(appId, sessionId);
        return connectionStateMap.get(sessionId);
    }

    public ConnectionState getConnectionState(String sessionId) {
        return connectionStateMap.get(sessionId);
    }

    public boolean tryAcquire(Long appId, String sessionId) {
        Object mutex = appMutexMap.computeIfAbsent(appId, key -> new Object());
        synchronized (mutex) {
            ConnectionState connectionState = connectionStateMap.get(sessionId);
            if (connectionState == null) {
                return false;
            }
            ChatLockState currentLock = appLockMap.get(appId);
            if (currentLock != null && !Objects.equals(currentLock.getSessionId(), sessionId)) {
                connectionState.setAccessMode("viewer");
                return false;
            }
            ChatLockState nextLock = ChatLockState.builder()
                    .appId(appId)
                    .sessionId(sessionId)
                    .userId(connectionState.getUserId())
                    .userName(connectionState.getUserName())
                    .userAvatar(connectionState.getUserAvatar())
                    .memberRole(connectionState.getMemberRole())
                    .occupyStartTime(LocalDateTime.now())
                    .build();
            appLockMap.put(appId, nextLock);
            connectionState.setAccessMode("editor");
            return true;
        }
    }

    public boolean releaseIfOccupant(String sessionId) {
        ConnectionState connectionState = connectionStateMap.get(sessionId);
        if (connectionState == null) {
            return false;
        }
        Long appId = connectionState.getAppId();
        Object mutex = appMutexMap.computeIfAbsent(appId, key -> new Object());
        synchronized (mutex) {
            ChatLockState currentLock = appLockMap.get(appId);
            if (currentLock == null || !Objects.equals(currentLock.getSessionId(), sessionId)) {
                connectionState.setAccessMode("viewer");
                return false;
            }
            appLockMap.remove(appId);
            connectionState.setAccessMode("viewer");
            return true;
        }
    }

    public void removeConnection(String sessionId) {
        connectionStateMap.remove(sessionId);
    }

    public boolean isOccupant(Long appId, Long userId) {
        ChatLockState currentLock = appLockMap.get(appId);
        return currentLock != null && Objects.equals(currentLock.getUserId(), userId);
    }

    public AppChatStateVO getChatState(Long appId) {
        ChatLockState lockState = appLockMap.get(appId);
        int totalConnections = (int) connectionStateMap.values().stream()
                .filter(connectionState -> Objects.equals(connectionState.getAppId(), appId))
                .count();
        int viewerCount = lockState == null ? totalConnections : Math.max(totalConnections - 1, 0);
        if (lockState == null) {
            return AppChatStateVO.builder()
                    .appId(appId)
                    .occupied(false)
                    .viewerCount(viewerCount)
                    .build();
        }
        return AppChatStateVO.builder()
                .appId(appId)
                .occupied(true)
                .occupyUserId(lockState.getUserId())
                .occupyUserName(lockState.getUserName())
                .occupyUserAvatar(lockState.getUserAvatar())
                .occupyMemberRole(lockState.getMemberRole())
                .occupyStartTime(lockState.getOccupyStartTime())
                .viewerCount(viewerCount)
                .build();
    }

    @Data
    @Builder
    public static class ConnectionState {

        private Long appId;

        private String sessionId;

        private Long userId;

        private String userName;

        private String userAvatar;

        private String memberRole;

        /**
         * editor / viewer
         */
        private String accessMode;
    }

    @Data
    @Builder
    @AllArgsConstructor
    private static class ChatLockState {

        private Long appId;

        private String sessionId;

        private Long userId;

        private String userName;

        private String userAvatar;

        private String memberRole;

        private LocalDateTime occupyStartTime;
    }
}
