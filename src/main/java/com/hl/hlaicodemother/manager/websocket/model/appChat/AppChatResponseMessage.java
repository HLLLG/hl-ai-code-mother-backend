package com.hl.hlaicodemother.manager.websocket.model.appChat;

import com.hl.hlaicodemother.model.vo.UserVO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 应用对话响应消息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppChatResponseMessage {

    /**
     * 响应类型， 例如：”INFO", "ERROR", "ENTER_CHAT", "EXIT_CHAT", "CHAT_ACTION"
     */
    private String type;

    /**
     * 消息
     */
    private String message;

    /**
     * 用户信息
     */
    private UserVO user;

    /**
     * 当 type 为 CHAT_STREAM 时：阶段 START / CHUNK / DONE / ERROR / STOPPED
     */
    private String streamPhase;

    /**
     * 当 type 为 CHAT_STREAM 时：本轮对话流标识（UUID）
     */
    private String streamId;

    /**
     * 当 type 为 CHAT_STREAM 时：阶段相关 JSON 字符串
     */
    private String streamPayload;
}
