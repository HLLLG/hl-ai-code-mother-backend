package com.hl.hlaicodemother.config;

import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 深度思考流式模型配置
 *
 * @author hl
 * @date 2023/7/27
 */
@Configuration
@ConfigurationProperties(prefix = "langchain4j.open-ai.chat-model")
@Data
public class ReasoningStreamingChatModelConfig {

    private String apiKey;

    private String baseUrl;

    @Bean
    public StreamingChatModel reasoningStreamingChatModel() {
        final String modelName = "deepseek-chat";
        final int maxToken = 8192;
        // 生产环境
//        final String modelName = "deepseek-reasoner";
//        final int maxToken = 32768;
        return OpenAiStreamingChatModel.builder()
                .modelName(modelName)
                .maxTokens(maxToken)
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .timeout(Duration.ofSeconds(120))
                .logRequests(true)
                .logResponses(true)
                .returnThinking(true) // 开启思考结果内容返回
                .build();
    }
}
