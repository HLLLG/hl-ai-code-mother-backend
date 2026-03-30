package com.hl.hlaicodemother.model.dto.appChat;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * WebSocket 协作对话指令
 */
@Data
public class AppChatCommandRequest implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * enter / leave
     */
    private String action;
}
