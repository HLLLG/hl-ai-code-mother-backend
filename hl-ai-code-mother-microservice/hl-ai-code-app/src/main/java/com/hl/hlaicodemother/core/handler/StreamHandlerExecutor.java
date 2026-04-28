package com.hl.hlaicodemother.core.handler;

import com.hl.hlaicodemother.ai.AiGenerationTaskManager;
import com.hl.hlaicodemother.manager.websocket.AppChatWebSocketHandler;
import com.hl.hlaicodemother.model.entity.User;
import com.hl.hlaicodemother.model.enums.CodeGenTypeEnum;
import com.hl.hlaicodemother.model.vo.UserVO;
import com.hl.hlaicodemother.service.ChatHistoryService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

/**
 * 流处理器执行器(执行器模式)
 */
@Slf4j
@Component
public class StreamHandlerExecutor {

    @Resource
    private JsonMessageStreamHandler jsonMessageStreamHandler;

    /**
     * 执行流处理器
     *
     * @param originFlux 原始流
     * @param appId 应用 ID
     * @param streamId 流 ID
     * @param user 当前用户信息
     * @param taskKey 任务键
     * @param editorVo 编辑者的用户视图对象
     * @param appChatWebSocketHandler WebSocket 处理器
     * @param chatHistoryService 聊天历史服务
     * @param aiGenerationTaskManager AI 生成任务管理器
     * @param codeGenTypeEnum 代码生成类型枚举
     */
    public Flux<String> doExecute(Flux<String> originFlux, Long appId, String streamId, User user, String taskKey,
                                UserVO editorVo, AppChatWebSocketHandler appChatWebSocketHandler,
                                ChatHistoryService chatHistoryService,
                                AiGenerationTaskManager aiGenerationTaskManager, CodeGenTypeEnum codeGenTypeEnum) {
        return switch (codeGenTypeEnum) {
            case VUE_PROJECT -> // 使用注入的组件实例
                    jsonMessageStreamHandler.handle(originFlux, appId, streamId, user, taskKey, editorVo,
                            appChatWebSocketHandler, chatHistoryService, aiGenerationTaskManager);
            case HTML, MULTI_FILE -> // 简单文本处理器不需要依赖注入
                    new SimpleTextStreamHandler().handle(originFlux, appId, streamId, user, taskKey, editorVo,
                            appChatWebSocketHandler, chatHistoryService, aiGenerationTaskManager);
        };
    }
}
