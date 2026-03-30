package com.hl.hlaicodemother.model.dto.appChat;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用协作对话事件
 */
@Data
@Builder
public class AppChatEvent implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String eventType;

    private Long appId;

    private LocalDateTime eventTime;

    private Object data;
}
