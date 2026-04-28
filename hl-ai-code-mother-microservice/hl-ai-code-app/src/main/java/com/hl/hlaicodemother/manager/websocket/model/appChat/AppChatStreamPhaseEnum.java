package com.hl.hlaicodemother.manager.websocket.model.appChat;

/**
 * 协作围观：对话流式同步阶段（与 {@link AppChatMessageTypeEnum#CHAT_STREAM} 配合使用）
 */
public enum AppChatStreamPhaseEnum {

    START,
    CHUNK,
    DONE,
    ERROR,
    STOPPED;

    public String getValue() {
        return name();
    }
}
