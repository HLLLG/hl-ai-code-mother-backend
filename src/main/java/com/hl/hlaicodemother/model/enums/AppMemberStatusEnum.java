package com.hl.hlaicodemother.model.enums;

import cn.hutool.core.util.ObjUtil;
import lombok.Getter;

/**
 * 应用成员状态枚举
 */
@Getter
public enum AppMemberStatusEnum {

    PENDING("待接受", 0),
    ACTIVE("已加入", 1),
    REMOVED("已移除", 2),
    REJECTED("已拒绝", 3);

    private final String text;

    private final Integer value;

    AppMemberStatusEnum(String text, Integer value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     *
     * @param value 状态值
     * @return 状态枚举
     */
    public static AppMemberStatusEnum getEnumByValue(Integer value) {
        if (ObjUtil.isEmpty(value)) {
            return null;
        }
        for (AppMemberStatusEnum statusEnum : AppMemberStatusEnum.values()) {
            if (statusEnum.value.equals(value)) {
                return statusEnum;
            }
        }
        return null;
    }
}
