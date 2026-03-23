package com.hl.hlaicodemother.core;

import cn.hutool.core.util.StrUtil;
import com.hl.hlaicodemother.ai.AiCodeGeneratorService;
import com.hl.hlaicodemother.ai.model.HtmlCodeResult;
import com.hl.hlaicodemother.ai.model.MultiFileCodeResult;
import com.hl.hlaicodemother.core.parser.CodeParserExecutor;
import com.hl.hlaicodemother.core.saver.CodeFileSaverExecutor;
import com.hl.hlaicodemother.exception.BusinessException;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.model.enums.CodeGenTypeEnum;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成器门面类
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    /**
     * 统一入口，根据类型生成并保存代码
     *
     * @param userMessage
     * @param codeGenType
     * @return
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenType) {
        // 校验参数
        if (StrUtil.isBlank(userMessage)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户输入不能为空");
        }
        if (codeGenType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
        }
        // 根据类型生成代码
        return switch (codeGenType) {
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(htmlCodeResult, CodeGenTypeEnum.HTML);
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(multiFileCodeResult, CodeGenTypeEnum.MULTI_FILE);
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
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenType) {
        // 校验参数
        if (StrUtil.isBlank(userMessage)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户输入不能为空");
        }
        if (codeGenType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
        }
        // 根据类型生成代码
        return switch (codeGenType) {
            case HTML -> {
                // 调用 AI 生成 HTML 代码结果（流式）
                Flux<String> htmlCodeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(htmlCodeStream, CodeGenTypeEnum.HTML);
            }
            case MULTI_FILE -> {
                // 调用 AI 生成多文件代码结果（流式）
                Flux<String> multiFileCodeStream = aiCodeGeneratorService.generateMultiFileStream(userMessage);
                yield processCodeStream(multiFileCodeStream, CodeGenTypeEnum.MULTI_FILE);
            }
            default -> {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的代码生成类型: " + codeGenType.getValue());
            }
        };
    }

    /**
     * 处理流式代码生成结果，实时接收代码片段并在完成后保存完整代码
     * @param codeStream
     * @param codeGenType
     * @return
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType) {
        // 当流式返沪生成代码完成后，再保存代码
        StringBuilder codeBuilder = new StringBuilder();
        return codeStream.doOnNext(chunk -> {
            // 实时手机代码片段
            codeBuilder.append(chunk);
        }).doOnComplete(() -> {
            // 流式生成完成后，保存代码到本地
            try {
                String completeResult = codeBuilder.toString();
                Object parserResult = CodeParserExecutor.executeParser(completeResult, codeGenType);
                File saveDir = CodeFileSaverExecutor.executeSaver(parserResult, codeGenType);
                log.info("保存成功，路径为：{}", saveDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存失败：{}", e.getMessage());
            }
        });
    }
}
