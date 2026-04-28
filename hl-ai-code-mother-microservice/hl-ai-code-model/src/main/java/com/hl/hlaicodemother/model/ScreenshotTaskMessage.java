package com.hl.hlaicodemother.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 部署后异步截图任务消息体（JSON 序列化），避免 URL 中含逗号时与分隔符冲突。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScreenshotTaskMessage {

    private Long appId;
    private String appDeployUrl;
}
