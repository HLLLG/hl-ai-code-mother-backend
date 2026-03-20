package com.hl.hlaicodemother.model.dto.user;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户注册请求封装类
 */
@Data
public class UserRegisterRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 用户账号
     */
    private String userAccount;

    /**
     * 用户密码
     */
    private String userPassword;

    /**
     * 检查密码
     */
    private String checkPassword;
}
