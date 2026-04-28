package com.hl.hlaicodemother.ai.model.message;

import lombok.Getter;

/**
 * 流消息类型枚举
 */
@Getter
public enum StreamMessageTypeEnum {

    AI_RESPONSE("ai_response", "AI响应"),
    TOOL_REQUEST("tool_request", "工具请求"),
    TOOL_EXECUTED("tool_execution", "工具执行结果"),
    THINKING_RESPONSE("thinking", "思考结果");

    private final String value;

    private final String text;

    StreamMessageTypeEnum(String value, String text) {
        this.value = value;
        this.text = text;
    }

    /**
     * 根据值获取枚举
     *
     * @param value
     * @return
     */
    public static StreamMessageTypeEnum getByValue(String value) {
        for (StreamMessageTypeEnum item : values()) {
            if (item.value.equals(value)) {
                return item;
            }
        }
        return null;
    }
}
