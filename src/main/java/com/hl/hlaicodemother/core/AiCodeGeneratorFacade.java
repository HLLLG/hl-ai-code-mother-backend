package com.hl.hlaicodemother.core;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hl.hlaicodemother.ai.AiCodeGeneratorService;
import com.hl.hlaicodemother.ai.AiCodeGeneratorServiceFactory;
import com.hl.hlaicodemother.ai.AiGenerationTaskManager;
import com.hl.hlaicodemother.ai.model.HtmlCodeResult;
import com.hl.hlaicodemother.ai.model.MultiFileCodeResult;
import com.hl.hlaicodemother.ai.model.message.AiResponseMessage;
import com.hl.hlaicodemother.ai.model.message.ToolExecutedMessage;
import com.hl.hlaicodemother.ai.model.message.ToolRequestMessage;
import com.hl.hlaicodemother.core.parser.CodeParserExecutor;
import com.hl.hlaicodemother.core.saver.CodeFileSaverExecutor;
import com.hl.hlaicodemother.exception.BusinessException;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.model.entity.App;
import com.hl.hlaicodemother.model.enums.CodeGenTypeEnum;
import com.hl.hlaicodemother.service.AppVersionService;
import dev.langchain4j.service.TokenStream;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;
import java.time.Duration;

/**
 * AI 代码生成器门面类
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorServiceFactory aiCodeGeneratorServiceFactory;

    @Resource
    private AppVersionService appVersionService;

    @Resource
    private AiGenerationTaskManager aiGenerationTaskManager;


    /**
     * 统一入口，根据类型生成并保存代码
     *
     * @param userMessage
     * @param codeGenType
     * @return
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenType, App app, String taskKey) {
        // 校验参数
        if (StrUtil.isBlank(userMessage)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户输入不能为空");
        }
        if (codeGenType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
        }
        AiCodeGeneratorService aiCodeGeneratorService =
                aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(app.getId(), codeGenType);
        // 根据类型生成代码
        return switch (codeGenType) {
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(htmlCodeResult, CodeGenTypeEnum.HTML, app.getId(),
                        app.getCurrentVersion());
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(multiFileCodeResult, CodeGenTypeEnum.MULTI_FILE, app.getId()
                        , app.getCurrentVersion());
            }
            default -> {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的代码生成类型: " + codeGenType.getValue());
            }
        };
    }

    /**
     * 统一入口，根据类型生成并保存代码（流式版本）
     *
     * @param userMessage
     * @param codeGenType
     * @return
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenType, App app,
                                                  String taskKey) {
        // 校验参数
        if (StrUtil.isBlank(userMessage)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户输入不能为空");
        }
        if (codeGenType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
        }
        if (StrUtil.isBlank(taskKey)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "任务标识不能为空");
        }
        AiGenerationTaskManager.TaskContext taskContext = aiGenerationTaskManager.registerTask(taskKey);
        AiCodeGeneratorService aiCodeGeneratorService =
                aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(app.getId(), codeGenType);
        // 根据类型生成代码
        return switch (codeGenType) {
            case HTML -> {
                Flux<String> htmlCodeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage)
                        .takeUntilOther(taskContext.getCancelSignal());
                yield processCodeStream(htmlCodeStream, CodeGenTypeEnum.HTML, app, userMessage, taskKey, taskContext);
            }
            case MULTI_FILE -> {
                Flux<String> multiFileCodeStream = aiCodeGeneratorService.generateMultiFileStream(userMessage)
                        .takeUntilOther(taskContext.getCancelSignal());
                yield processCodeStream(multiFileCodeStream, CodeGenTypeEnum.MULTI_FILE, app, userMessage, taskKey,
                        taskContext);
            }
            case VUE_PROJECT -> {
                aiCodeGeneratorServiceFactory.resetGeneration(app.getId());
                TokenStream vueProjectStream = aiCodeGeneratorService.generateVueProjectStream(app.getId(), userMessage);
                yield processTokenStream(vueProjectStream, taskKey, taskContext, app.getId())
                        .takeUntilOther(taskContext.getCancelSignal());
            }
            default -> throw new BusinessException(ErrorCode.PARAMS_ERROR,
                    "不支持的代码生成类型: " + codeGenType.getValue());
        };
    }

    /**
     * 处理流式代码生成结果，实时接收代码片段并在完成后保存完整代码
     *
     * @param codeStream
     * @param codeGenType
     * @return
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, App app,
                                           String userMessage, String taskKey,
                                           AiGenerationTaskManager.TaskContext taskContext) {
        StringBuilder codeBuilder = new StringBuilder();
        // 实时收集代码片段
        return codeStream.doOnNext(codeBuilder::append).doOnComplete(() -> {
            if (taskContext.isCancelled()) {
                log.info("生成已手动停止，跳过保存，taskKey={}", taskKey);
                return;
            }
            try {
                String completeResult = codeBuilder.toString();
                Object parserResult = CodeParserExecutor.executeParser(completeResult, codeGenType);
                int version = appVersionService.addVersion(app, userMessage);
                File saveDir = CodeFileSaverExecutor.executeSaver(parserResult, codeGenType, app.getId(), version);
                log.info("保存成功，路径为：{}", saveDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存失败", e);
            }
        }).doFinally(signalType -> aiGenerationTaskManager.removeTask(taskKey, taskContext));
    }

    private static final Duration STREAM_TIMEOUT = Duration.ofMinutes(5);

    /**
     * 将TokenStream转换为Flux<String>, 并传递工具调用信息。
     * 通过 sink.onDispose 将 Flux 取消信号桥接到 taskContext 和 FileWriteTool，
     * 使得 FileWriteTool 抛出异常来中断 langchain4j 的工具调用循环，阻止后续 LLM 请求。
     * 添加了 STREAM_TIMEOUT 兜底：如果超时未收到任何数据则自动完成流。
     */
    private Flux<String> processTokenStream(TokenStream tokenStream, String taskKey,
                                            AiGenerationTaskManager.TaskContext taskContext,
                                            Long appId) {
        return Flux.<String>create(sink -> {
            sink.onDispose(() -> {
                taskContext.cancel();
                aiCodeGeneratorServiceFactory.cancelGeneration(appId);
            });

            tokenStream.onPartialResponse((partialResponse) -> {
                        if (taskContext.isCancelled()) return;
                        AiResponseMessage aiResponseMessage = new AiResponseMessage(partialResponse);
                        sink.next(JSONUtil.toJsonStr(aiResponseMessage));
                    })
                    .onPartialToolExecutionRequest((index, partialToolExecutionRequest) -> {
                        if (taskContext.isCancelled()) return;
                        ToolRequestMessage toolRequestMessage = new ToolRequestMessage(partialToolExecutionRequest);
                        sink.next(JSONUtil.toJsonStr(toolRequestMessage));
                    })
                    .onToolExecuted(toolExecution -> {
                        if (taskContext.isCancelled()) return;
                        ToolExecutedMessage toolExecutionMessage = new ToolExecutedMessage(toolExecution);
                        sink.next(JSONUtil.toJsonStr(toolExecutionMessage));
                    })
                    .onCompleteResponse(completeResponse -> {
                        log.info("TokenStream onCompleteResponse, taskKey={}", taskKey);
                        if (taskContext.isCancelled()) {
                            log.info("生成已手动停止，跳过保存，taskKey={}", taskKey);
                        }
                        sink.complete();
                    })
                    .onError(error -> {
                        if (taskContext.isCancelled()) {
                            log.info("生成已手动停止，忽略错误，taskKey={}", taskKey);
                            sink.complete();
                            return;
                        }
                        log.error("TokenStream 生成出错, taskKey={}", taskKey, error);
                        sink.error(error);
                    })
                    .start();
        })
        .timeout(STREAM_TIMEOUT)
        .onErrorResume(java.util.concurrent.TimeoutException.class, e -> {
            log.warn("TokenStream 超时（{}），强制结束流, taskKey={}", STREAM_TIMEOUT, taskKey);
            return Flux.empty();
        })
        .doFinally(signalType -> {
            log.info("TokenStream Flux doFinally, signal={}, taskKey={}", signalType, taskKey);
            aiGenerationTaskManager.removeTask(taskKey, taskContext);
        });
    }
}
