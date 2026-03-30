package com.hl.hlaicodemother.manager.websocket.model.appChat;

import cn.hutool.core.util.StrUtil;
import lombok.Getter;

/**
 * 应用协作对话消息类型枚举
 */
@Getter
public enum AppChatMessageTypeEnum {

    INFO("发送通知",  "INFO"),
    ERROR("发送错误", "ERROR"),
    ENTER_CHAT("进入聊天状态", "ENTER_CHAT"),
    EXIT_CHAT("退出聊天状态", "EXIT_CHAT"),
    CHAT_ACTION("执行聊天动作", "CHAT_ACTION");

    private final String text;

    private final String value;

    AppChatMessageTypeEnum(String text, String value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据 value 获取枚举
     */
    public static AppChatMessageTypeEnum getByValue(String value) {
        if (StrUtil.isBlank(value)) {
            return null;
        }
        for (AppChatMessageTypeEnum item : AppChatMessageTypeEnum.values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }
}
