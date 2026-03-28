package com.hl.hlaicodemother.core;

import cn.hutool.core.util.StrUtil;
import com.hl.hlaicodemother.ai.AiCodeGeneratorService;
import com.hl.hlaicodemother.ai.AiCodeGeneratorServiceFactory;
import com.hl.hlaicodemother.ai.model.HtmlCodeResult;
import com.hl.hlaicodemother.ai.model.MultiFileCodeResult;
import com.hl.hlaicodemother.core.parser.CodeParserExecutor;
import com.hl.hlaicodemother.core.saver.CodeFileSaverExecutor;
import com.hl.hlaicodemother.exception.BusinessException;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.exception.ThrowUtils;
import com.hl.hlaicodemother.model.entity.App;
import com.hl.hlaicodemother.model.entity.AppVersion;
import com.hl.hlaicodemother.model.enums.CodeGenTypeEnum;
import com.hl.hlaicodemother.service.AppService;
import com.hl.hlaicodemother.service.AppVersionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import com.hl.hlaicodemother.ai.AiGenerationTaskManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;

import java.io.File;

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
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenType, App app) {
        // 校验参数
        if (StrUtil.isBlank(userMessage)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户输入不能为空");
        }
        if (codeGenType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
        }
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(app.getId());
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
        AiCodeGeneratorService aiCodeGeneratorService = aiCodeGeneratorServiceFactory.getAiCodeGeneratorService(app.getId());
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
}
