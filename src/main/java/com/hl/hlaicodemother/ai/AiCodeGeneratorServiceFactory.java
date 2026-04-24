package com.hl.hlaicodemother.ai;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.hl.hlaicodemother.ai.guardrail.PromptSafetyInputGuardrail;
import com.hl.hlaicodemother.ai.guardrail.RetryOutputGuardrail;
import com.hl.hlaicodemother.ai.tools.ToolManager;
import com.hl.hlaicodemother.config.MyRedisChatMemoryStore;
import com.hl.hlaicodemother.exception.BusinessException;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.model.enums.CodeGenTypeEnum;
import com.hl.hlaicodemother.service.ChatHistoryService;
import com.hl.hlaicodemother.utils.SpringContextUtil;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ai 服务创建工厂
 */
@Component
@Slf4j
@Getter
public class AiCodeGeneratorServiceFactory {

    @Resource(name = "openAiChatModel")
    private ChatModel chatModel;

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
     * 每个 appId 对应的 Vue 文件工具聚合（写入/删除/读取/修改/目录列举），用于版本同步与取消任务。
     */
    private final ConcurrentHashMap<Long, ToolManager> vueProjectFileToolsMap = new ConcurrentHashMap<>();

    /**
     * 每个 appId 对应的可取消 StreamingChatModel 包装器引用，
     * 用于在取消任务时阻止新一轮 LLM 请求（中断 langchain4j 工具调用循环的关键）
     */
    private final ConcurrentHashMap<Long, CancellableStreamingChatModelWrapper> chatModelWrapperMap =
            new ConcurrentHashMap<>();

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
            case VUE_PROJECT -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel reasoningStreamingChatModel = SpringContextUtil.getBean(
                        "reasoningStreamingChatModelPrototype", StreamingChatModel.class);
                ToolManager toolManager = ToolManager.build(appId);
                vueProjectFileToolsMap.put(appId, toolManager);
                CancellableStreamingChatModelWrapper modelWrapper =
                        new CancellableStreamingChatModelWrapper(reasoningStreamingChatModel);
                chatModelWrapperMap.put(appId, modelWrapper);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .streamingChatModel(modelWrapper)
                        .chatMemoryProvider(memoryId -> chatMemory)
                        .tools(toolManager.getAllTools())
                        .hallucinatedToolNameStrategy(toolExecutionRequest ->
                                ToolExecutionResultMessage.from(toolExecutionRequest,
                                        "Error: that is no tool called " + toolExecutionRequest.name())
                        )
                        .inputGuardrails(new PromptSafetyInputGuardrail())
//                        .outputGuardrails(new RetryOutputGuardrail()) 可能会导致无法流式输出，所以关闭
                        .maxSequentialToolsInvocations(30) // 最多连续调用20次工具
                        .build();
            }
            case HTML, MULTI_FILE -> {
                // 使用多例模式的 StreamingChatModel 解决并发问题
                StreamingChatModel openAiStreamingChatModel = SpringContextUtil.getBean("streamingChatModelPrototype"
                        , StreamingChatModel.class);
                yield AiServices.builder(AiCodeGeneratorService.class)
                        .streamingChatModel(openAiStreamingChatModel)
                        .chatMemory(chatMemory)
                        .inputGuardrails(new PromptSafetyInputGuardrail())
//                        .outputGuardrails(new RetryOutputGuardrail()) 可能会导致无法流式输出，所以关闭
                        .build();
            }
            default ->
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }

    /**
     * 取消指定 appId 的生成：
     * 1. 标记 Vue 文件工具聚合为已取消
     * 2. 标记 StreamingChatModel 包装器为已取消 → 阻止新一轮 LLM 请求（中断工具调用循环）
     */
    public void cancelGeneration(Long appId) {
        ToolManager vueTools = vueProjectFileToolsMap.get(appId);
        if (vueTools != null) {
            vueTools.setCancelled(true);
        }
        CancellableStreamingChatModelWrapper wrapper = chatModelWrapperMap.get(appId);
        if (wrapper != null) {
            wrapper.setCancelled(true);
        }
    }

    /**
     * 设置 Vue 工具写入目录版本（vue_project_{appId}/v{n}/），并清空本轮已写文件记录。
     * 须在 {@link #resetGeneration(Long)} 之后、启动 TokenStream 之前调用。
     */
    public void configureVueOutputVersion(long appId, int version) {
        ToolManager vueTools = vueProjectFileToolsMap.get(appId);
        if (vueTools != null) {
            vueTools.prepareForGeneration(version);
        }
    }

    /**
     * 重置指定 appId 的取消状态，在新一轮生成前调用
     */
    public void resetGeneration(Long appId) {
        ToolManager vueTools = vueProjectFileToolsMap.get(appId);
        if (vueTools != null) {
            vueTools.setCancelled(false);
        }
        CancellableStreamingChatModelWrapper wrapper = chatModelWrapperMap.get(appId);
        if (wrapper != null) {
            wrapper.setCancelled(false);
        }
    }

    /**
     * 获取工具管理类
     */
    public ToolManager getToolManager(Long appId) {
        return vueProjectFileToolsMap.get(appId);
    }

    private String buildCachedKey(Long appId, CodeGenTypeEnum codeGenType) {
        return appId + "_" + codeGenType.getValue();
    }
}
