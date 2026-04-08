package com.hl.hlaicodemother.langgraph4j.ai;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 图片收集AI服务
 *
 * @author hl
 */
public interface ImageCollectionService {

    /**
     * 根据用户提示词手机所需的图片资源
     * Ai 会根据需求自主选择工具来完成图片资源获取
     *
     * @param userMessage 用户输入
     * @return 图片资源
     */
    @SystemMessage(fromResource = "prompt/image-collection-system-prompt.txt")
    String collectImages(@UserMessage String userMessage);
}
