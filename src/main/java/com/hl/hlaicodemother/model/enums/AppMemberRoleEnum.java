package com.hl.hlaicodemother.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 应用成员角色枚举
 */
@Getter
public enum AppMemberRoleEnum {

    OWNER("拥有者", "owner"),
    EDITOR("编辑者", "editor"),
    VIEWER("查看者", "viewer");

    private final String text;

    private final String value;

    AppMemberRoleEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 角色值
     * @return 角色枚举
     */
    public static AppMemberRoleEnum getEnumByValue(String value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (AppMemberRoleEnum roleEnum : AppMemberRoleEnum.values()) {
            if (roleEnum.value.equals(value)) {
                return roleEnum;
            }
        }
        return null;
    }
}
