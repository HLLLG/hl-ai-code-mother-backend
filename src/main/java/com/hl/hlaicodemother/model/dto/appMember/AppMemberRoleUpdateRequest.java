package com.hl.hlaicodemother.model.dto.appMember;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用成员角色更新请求
 */
@Data
public class AppMemberRoleUpdateRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 成员角色：editor/viewer
     */
    private String memberRole;
}
