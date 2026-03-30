package com.hl.hlaicodemother.model.vo;

import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用协作对话状态
 */
@Data
@Builder
public class AppChatStateVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long appId;

    private boolean occupied;

    private Long occupyUserId;

    private String occupyUserName;

    private String occupyUserAvatar;

    private String occupyMemberRole;

    private LocalDateTime occupyStartTime;

    private Integer viewerCount;
}
