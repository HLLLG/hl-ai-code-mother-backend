package com.hl.hlaicodemother.core.handler;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hl.hlaicodemother.ai.AiCodeGeneratorServiceFactory;
import com.hl.hlaicodemother.ai.AiGenerationTaskManager;
import com.hl.hlaicodemother.ai.model.message.*;
import com.hl.hlaicodemother.ai.tools.BaseTool;
import com.hl.hlaicodemother.ai.tools.ToolManager;
import com.hl.hlaicodemother.constant.AppConstant;
import com.hl.hlaicodemother.core.builder.VueProjectBuilder;
import com.hl.hlaicodemother.manager.websocket.AppChatWebSocketHandler;
import com.hl.hlaicodemother.manager.websocket.model.appChat.AppChatStreamPhaseEnum;
import com.hl.hlaicodemother.model.entity.AppVersion;
import com.hl.hlaicodemother.model.entity.User;
import com.hl.hlaicodemother.model.enums.ChatHistoryMessageTypeEnum;
import com.hl.hlaicodemother.model.vo.UserVO;
import com.hl.hlaicodemother.service.AppVersionService;
import com.hl.hlaicodemother.service.ChatHistoryService;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * JSON 消息流处理器
 */
@Slf4j
@Component
public class JsonMessageStreamHandler {

    @Resource
    private VueProjectBuilder vueProjectBuilder;

    @Resource
    private AppVersionService appVersionService;

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    /**
     * 处理 AI 生成的 JSON 消息流，将原始 Flux 流转换为处理后的字符串流
     * 主要功能包括：解析 JSON 消息块、广播给围观成员、保存聊天历史、处理完成/取消/异常事件
     *
     * @param originFlux              原始 AI 生成的消息流
     * @param appId                   应用 ID
     * @param streamId                流 ID，用于标识当前的生成任务
     * @param user                    当前用户信息
     * @param taskKey                 任务键，用于从任务管理器中获取任务上下文
     * @param editorVo                编辑者的用户视图对象，用于 WebSocket 广播
     * @param appChatWebSocketHandler WebSocket 处理器，用于向客户端广播消息
     * @param chatHistoryService      聊天历史服务，用于保存 AI 响应到历史记录
     * @param aiGenerationTaskManager AI 生成任务管理器，用于检查任务是否被取消
     * @return 处理后的消息流，包含处理过的 JSON 字符串
     */
    Flux<String> handle(Flux<String> originFlux, Long appId, String streamId, User user, String taskKey,
                        UserVO editorVo, AppChatWebSocketHandler appChatWebSocketHandler,
                        ChatHistoryService chatHistoryService, AiGenerationTaskManager aiGenerationTaskManager) {
        // 在 Flux 组装阶段持有 TaskContext 引用，避免 doFinally 先于 doOnComplete 将其从 map 中移除导致查找不到
        AiGenerationTaskManager.TaskContext taskContext = aiGenerationTaskManager.getTaskContext(taskKey);
        // 累积 AI 响应的完整内容，用于保存到聊天历史
        StringBuilder chatHistoryBuilder = new StringBuilder();
        // 记录已见过的工具 ID，用于判断是否为首次调用该工具
        Set<String> seenTollIds = new HashSet<>();
        return originFlux
                // 切换到弹性调度器，避免阻塞主线程
                .publishOn(Schedulers.boundedElastic())
                // 处理每个生成的代码块：构建消息并广播给围观成员
                .map(chunk -> {
                    // 解析每个 JSON 消息块
                    return handleJsonMessageChunk(chunk, appId, streamId, user, editorVo, appChatWebSocketHandler,
                            chatHistoryBuilder, seenTollIds);
                }).filter(StrUtil::isNotEmpty) // 过滤空字符串
                // 处理生成完成事件：根据是否被取消发送不同状态，并保存 AI 响应到聊天历史
                .doOnComplete(() -> {
                    boolean cancelled = taskContext != null && taskContext.isCancelled();
                    if (cancelled) {
                        // 任务被取消，发送停止消息
                        String stopPayLoad = JSONUtil.toJsonStr(Map.of("message", "本次生成已停止。"));
                        appChatWebSocketHandler.broadcastToApp(appId, user, streamId, stopPayLoad,
                                AppChatStreamPhaseEnum.STOPPED.getValue(), editorVo);
                    } else {
                        // 异步构造 Vue 项目
                        int versionCount = (int) appVersionService.count(new QueryWrapper().eq(AppVersion::getAppId,
                                appId));
                        String projectPath =
                                AppConstant.CODE_OUTPUT_ROOT_DIR + "/vue_project_" + appId + "/v" + versionCount;
                        vueProjectBuilder.buildProjectAsync(projectPath);
                        // 任务正常完成，发送完成消息并提示刷新应用
                        String donePayLoad = JSONUtil.toJsonStr(Map.of("refreshApp", true));
                        appChatWebSocketHandler.broadcastToApp(appId, user, streamId, donePayLoad,
                                AppChatStreamPhaseEnum.DONE.getValue(), editorVo);
                    }
                    // 保存完整的 AI 响应到聊天历史
                    String aiResponse = chatHistoryBuilder.toString();
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


    private String handleJsonMessageChunk(String chunk, Long appId, String streamId, User user, UserVO editorVo,
                                          AppChatWebSocketHandler appChatWebSocketHandler,
                                          StringBuilder chatHistoryBuilder, Set<String> seenTollIds) {
        // 获取当前应用的工具管理器
        ToolManager toolManager = aiCodeGeneratorServiceFactory.getToolManager(appId);
        // 将chunk 解析为 StreamMessage 对象
        StreamMessage streamMessage = JSONUtil.toBean(chunk, StreamMessage.class);
        String type = streamMessage.getType();
        StreamMessageTypeEnum messageTypeEnum = StreamMessageTypeEnum.getByValue(type);
        switch (messageTypeEnum) {
            case AI_RESPONSE -> {
                // 将 chunk 转换为 AiResponseMessage 对象
                AiResponseMessage aiResponseMessage = JSONUtil.toBean(chunk, AiResponseMessage.class);
                String data = aiResponseMessage.getData();
                String chunkPayLoad = JSONUtil.toJsonStr(Map.of("d", data));
                appChatWebSocketHandler.broadcastToApp(appId, user, streamId, chunkPayLoad,
                        AppChatStreamPhaseEnum.CHUNK.getValue(), editorVo);
                chatHistoryBuilder.append(data);
                return data;
            }
            case THINKING_RESPONSE -> {
                // 将 chunk 转换为 ThinkingResponseMessage 对象
                ThinkingResponseMessage thinkingResponseMessage = JSONUtil.toBean(chunk, ThinkingResponseMessage.class);
                String text = thinkingResponseMessage.getText();
                String output = String.format("[正在思考]：%s", text);
                String chunkPayLoad = JSONUtil.toJsonStr(Map.of("d", output));
                appChatWebSocketHandler.broadcastToApp(appId, user, streamId, chunkPayLoad,
                        AppChatStreamPhaseEnum.CHUNK.getValue(), editorVo);
                chatHistoryBuilder.append(output);
                return output;
            }
            case TOOL_REQUEST -> {
                // 将 chunk 转换为 ToolRequestMessage 对象
                ToolRequestMessage toolRequestMessage = JSONUtil.toBean(chunk, ToolRequestMessage.class);
                String id = toolRequestMessage.getId();
                String toolName = toolRequestMessage.getName();
                // 检查是否是第一次调用工具
                if (id != null && !seenTollIds.contains(id)) {
                    seenTollIds.add(id);
                    BaseTool tool = toolManager.getTool(toolName);
                    String toolRequestResponse = tool.generateToolRequestResponse();
                    String chunkPayLoad = JSONUtil.toJsonStr(Map.of("d", toolRequestResponse));
                    appChatWebSocketHandler.broadcastToApp(appId, user, streamId, chunkPayLoad,
                            AppChatStreamPhaseEnum.CHUNK.getValue(), editorVo);
                    return toolRequestResponse;
                } else {
                    return "";
                }
            }
            case TOOL_EXECUTED -> {
                // 将 chunk 转换为 ToolExecutionMessage 对象
                ToolExecutedMessage toolExecutedMessage = JSONUtil.toBean(chunk, ToolExecutedMessage.class);
                String toolName = toolExecutedMessage.getName();
                JSONObject arguments = JSONUtil.parseObj(toolExecutedMessage.getArguments());
                BaseTool tool = toolManager.getTool(toolName);
                String result = tool.generateToolExecuteResult(arguments);
                // 输出前端和要持久化的内容
                String output = String.format("\n\n%s\n\n", result);
                String chunkPayLoad = JSONUtil.toJsonStr(Map.of("d", output));
                appChatWebSocketHandler.broadcastToApp(appId, user, streamId, chunkPayLoad,
                        AppChatStreamPhaseEnum.CHUNK.getValue(), editorVo);
                chatHistoryBuilder.append(output);
                return output;
            }
            default -> {
                log.error("不支持的消息类型：{}", type);
                return "";
            }
        }
    }
}
