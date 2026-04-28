package com.hl.hlaicodemother.model.dto.appMember;

import com.hl.hlaicodemother.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 应用成员查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class AppMemberQueryRequest extends PageRequest implements Serializable {

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
}
