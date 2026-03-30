package com.hl.hlaicodemother.model.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * WebSocket 连接后的对话身份快照
 */
@Data
@Builder
public class AppChatConnectionVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private String accessMode;

    private AppChatStateVO state;
}
