package com.hl.hlaicodemother.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hl.hlaicodemother.ai.tools.FileWriteTool;
import com.hl.hlaicodemother.config.MyRedisChatMemoryStore;
import com.hl.hlaicodemother.exception.BusinessException;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.model.enums.CodeGenTypeEnum;
import com.hl.hlaicodemother.service.ChatHistoryService;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.response.PartialToolCall;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolExecution;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * ai 服务创建工厂
 */
@Component
@Slf4j
public class AiCodeGeneratorServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private StreamingChatModel openAiStreamingChatModel;

    @Resource
    private StreamingChatModel reasoningStreamingChatModel;

    @Resource
    private MyRedisChatMemoryStore chatMemoryStore;

    @Resource
    private ChatHistoryService chatHistoryService;


    /**
     * AI 服务实例缓存
     */
    private final Cache<String, AiCodeGeneratorService> serviceCache = Caffeine.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofMinutes(30))
            .expireAfterAccess(Duration.ofMinutes(10))
            .removalListener((key, value, cause) ->
                    log.info("Ai 服务实例被移除，appId {}，原因： {}", key, cause))
            .build();

    /**
     * 获取 AI服务实例（兼容老逻辑）
     *
     * @param appId
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId) {
        return getAiCodeGeneratorService(appId, CodeGenTypeEnum.HTML);
    }

    /**
     * 获取 AI服务实例
     *
     * @param appId
     * @return
     */
    public AiCodeGeneratorService getAiCodeGeneratorService(long appId, CodeGenTypeEnum codeGenType) {
        String cachedKey = buildCachedKey(appId, codeGenType);
        return serviceCache.get(cachedKey, key -> createAiCodeGeneratorService(appId, codeGenType));
    }

    /**
     * 创建 AI服务实例
     *
     * @param appId
     * @return
     */
    private AiCodeGeneratorService createAiCodeGeneratorService(Long appId, CodeGenTypeEnum codeGenType) {
        log.info("为 appId: {} 创建新的 AI 服务实例", appId);
        MessageWindowChatMemory chatMemory = MessageWindowChatMemory.builder()
                .maxMessages(20)
                .id(appId)
                .chatMemoryStore(chatMemoryStore)
                .build();
        // 从数据库加载最近的对话历史到内存中
        chatHistoryService.loadChatHistoryToMemory(appId, 20, chatMemory);

        // 构建 AI 服务实例
        return switch (codeGenType) {
            case VUE_PROJECT -> AiServices.builder(AiCodeGeneratorService.class)
                    .chatModel(chatModel)
                    .streamingChatModel(reasoningStreamingChatModel)
                    .chatMemoryProvider(memoryId -> chatMemory)
                    .tools(new FileWriteTool())
                    .hallucinatedToolNameStrategy(toolExecutionRequest ->
                            ToolExecutionResultMessage.from(toolExecutionRequest,
                                    "Error: that is no tool called " + toolExecutionRequest.name())
                    )
                    .build();
            case HTML, MULTI_FILE -> AiServices.builder(AiCodeGeneratorService.class)
                    .chatModel(chatModel)
                    .streamingChatModel(openAiStreamingChatModel)
                    .chatMemory(chatMemory)
                    .build();
            default ->
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }

    private String buildCachedKey(Long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }
}
