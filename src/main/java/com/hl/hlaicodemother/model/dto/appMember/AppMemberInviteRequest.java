package com.hl.hlaicodemother.model.dto.appMember;

import lombok.Data;

import java.io.Serializable;

/**
 * 邀请应用成员请求
 */
@Data
public class AppMemberInviteRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 被邀请用户 id
     */
    private Long userId;

    /**
     * 成员角色：editor/viewer
     */
    private String memberRole;
}
