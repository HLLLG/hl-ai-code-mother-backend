package com.hl.hlaicodemother.ai;

import com.hl.hlaicodemother.utils.SpringContextUtil;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AiCodeGenTypeRoutingServiceFactory {

    /**
     * 创建 AiCodeGenTypeRoutingService 实例
     *
     * @return
     */
    private AiCodeGenTypeRoutingService createAiCodeGenTypeRoutingService() {
        // 从配置中获取路由模型实例
        ChatModel chatModel = SpringContextUtil.getBean("routingChatModelPrototype", ChatModel.class);
        return AiServices.builder(AiCodeGenTypeRoutingService.class)
                .chatModel(chatModel)
                .build();
    }

    /**
     * 默认提供一个bean
     *
     * @return
     */
    public AiCodeGenTypeRoutingService aiCodeGenTypeRoutingService() {
        return createAiCodeGenTypeRoutingService();
    }

}
