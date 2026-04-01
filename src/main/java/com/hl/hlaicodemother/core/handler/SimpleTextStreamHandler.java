package com.hl.hlaicodemother.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hl.hlaicodemother.ai.AiGenerationTaskManager;
import com.hl.hlaicodemother.manager.websocket.AppChatWebSocketHandler;
import com.hl.hlaicodemother.manager.websocket.model.appChat.AppChatStreamPhaseEnum;
import com.hl.hlaicodemother.model.entity.User;
import com.hl.hlaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.hl.hlaicodemother.model.vo.UserVO;
import com.hl.hlaicodemother.service.ChatHistoryService;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Map;

/**
 * 原生文本流处理器
 * 处理 HTML 和 MULTI_FILE 类型的流式响应
 */
@Slf4j
public class SimpleTextStreamHandler {

    Flux<String> handle(Flux<String> originFlux, Long appId, String streamId, User user, String taskKey,
                        UserVO editorVo, AppChatWebSocketHandler appChatWebSocketHandler,
                        ChatHistoryService chatHistoryService, AiGenerationTaskManager aiGenerationTaskManager) {
        StringBuilder chunkBuilder = new StringBuilder();
        return originFlux
                // 切换到弹性调度器，避免阻塞主线程
                .publishOn(Schedulers.boundedElastic())
                // 累积生成的代码块，用于后续保存到聊天历史
                // 处理每个生成的代码块：构建消息并广播给围观成员
                .map(chunk -> {
                    String chunkPayLoad = JSONUtil.toJsonStr(Map.of("d", chunk));
                    appChatWebSocketHandler.broadcastToApp(appId, user, streamId, chunkPayLoad,
                            AppChatStreamPhaseEnum.CHUNK.getValue(), editorVo);
                    chunkBuilder.append(chunk);
                    return chunk;
                })
                // 处理生成完成事件：根据是否被取消发送不同状态，并保存 AI 响应到聊天历史
                .doOnComplete(() -> {
                    AiGenerationTaskManager.TaskContext ctx = aiGenerationTaskManager.getTaskContext(taskKey);
                    boolean cancelled = ctx != null && ctx.isCancelled();
                    if (cancelled) {
                        // 任务被取消，发送停止消息
                        String stopPayLoad = JSONUtil.toJsonStr(Map.of("message", "本次生成已停止。"));
                        appChatWebSocketHandler.broadcastToApp(appId, user, streamId, stopPayLoad,
                                AppChatStreamPhaseEnum.STOPPED.getValue(), editorVo);
                    } else {
                        // 任务正常完成，发送完成消息并提示刷新应用
                        String donePayLoad = JSONUtil.toJsonStr(Map.of("refreshApp", true));
                        appChatWebSocketHandler.broadcastToApp(appId, user, streamId, donePayLoad,
                                AppChatStreamPhaseEnum.DONE.getValue(), editorVo);
                    }
                    // 保存完整的 AI 响应到聊天历史
                    String aiResponse = chunkBuilder.toString();
                    if (StrUtil.isNotBlank(aiResponse)) {
                        chatHistoryService.addChatMessage(appId, aiResponse, ChatHistoryMessageTypeEnum.AI.getValue()
                                , user.getId());
                    }
                })
                // 处理生成过程中的异常：记录日志、广播错误消息并保存错误信息到聊天历史
                .doOnError(e -> {
                    log.error("AI 生成代码出错", e);
                    String errPayLoad = JSONUtil.toJsonStr(Map.of("message", "生成失败：" + (e.getMessage() == null ?
                            "未知错误" : e.getMessage())));
                    appChatWebSocketHandler.broadcastToApp(appId, user, streamId, errPayLoad,
                            AppChatStreamPhaseEnum.ERROR.getValue(), editorVo);
                    String errorMessage = "AI 生成代码出错：" + e.getMessage();
                    chatHistoryService.addChatMessage(appId, errorMessage, ChatHistoryMessageTypeEnum.AI.getValue(),
                            user.getId());
                });
    }
}
