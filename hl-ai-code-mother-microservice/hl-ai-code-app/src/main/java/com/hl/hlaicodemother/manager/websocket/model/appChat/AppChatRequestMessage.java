package com.hl.hlaicodemother.manager.websocket.model.appChat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * WebSocket 协作对话指令
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AppChatRequestMessage implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 消息类型，例如 "ENTER_CHAT", "EXIT_CHAT", "CHAT_ACTION"
     */
    private String type;
}
