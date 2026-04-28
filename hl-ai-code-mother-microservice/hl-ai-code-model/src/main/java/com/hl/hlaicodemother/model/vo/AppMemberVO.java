package com.hl.hlaicodemother.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 应用成员视图对象
 */
@Data
public class AppMemberVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * id
     */
    private Long id;

    /**
     * 应用 id
     */
    private Long appId;

    /**
     * 用户 id
     */
    private Long userId;

    /**
     * 成员角色
     */
    private String memberRole;

    /**
     * 成员状态
     */
    private Integer memberStatus;

    /**
     * 邀请人用户 id
     */
    private Long invitedBy;

    /**
     * 最后活跃时间
     */
    private LocalDateTime lastActiveTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    private LocalDateTime updateTime;

    /**
     * 成员用户信息
     */
    private UserVO user;
}
