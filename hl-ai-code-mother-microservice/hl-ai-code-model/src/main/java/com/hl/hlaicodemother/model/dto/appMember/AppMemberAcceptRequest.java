package com.hl.hlaicodemother.model.dto.appMember;

import lombok.Data;

import java.io.Serializable;

/**
 * 接受应用成员邀请请求
 */
@Data
public class AppMemberAcceptRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 应用 id
     */
    private Long appId;
}
