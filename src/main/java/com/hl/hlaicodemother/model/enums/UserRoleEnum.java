package com.hl.hlaicodemother.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 用户枚举类
 */
@Getter
public enum UserRoleEnum {

    USER("普通用户", "user" ),
    ADMIN("管理员", "admin");

    private String text;
    private String value;

    UserRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取用户枚举
     * @param value
     * @return
     */
    public static UserRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (UserRoleEnum role : UserRoleEnum.values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        return null;
    }
}
