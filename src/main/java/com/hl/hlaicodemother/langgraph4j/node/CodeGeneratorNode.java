package com.hl.hlaicodemother.langgraph4j.node;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.hl.hlaicodemother.constant.AppConstant;
import com.hl.hlaicodemother.core.AiCodeGeneratorFacade;
import com.hl.hlaicodemother.langgraph4j.model.QualityResult;
import com.hl.hlaicodemother.langgraph4j.state.WorkflowContext;
import com.hl.hlaicodemother.model.entity.App;
import com.hl.hlaicodemother.model.enums.CodeGenTypeEnum;
import com.hl.hlaicodemother.service.AppService;
import com.hl.hlaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;
import reactor.core.publisher.Flux;

import java.time.Duration;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 代码生成节点
 */
@Slf4j
public class CodeGeneratorNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 代码生成");
            CodeGenTypeEnum generationType = context.getGenerationType();
            // 构建用户提示
            String userMessage = buildUserMessage(context);

            // 获取 AI 代码生成门面服务
            AiCodeGeneratorFacade aiCodeGeneratorFacade = SpringContextUtil.getBean(AiCodeGeneratorFacade.class);
            log.info("开始生成代码，类型：{} （{}）", generationType.getValue(), generationType.getText());
            // 先使用固定的APP
            AppService appService = SpringContextUtil.getBean(AppService.class);
            App oldApp = appService.getById(398280987424382976L);
            Flux<String> codeStream = aiCodeGeneratorFacade.generateAndSaveCodeStream(userMessage, generationType,
                    oldApp, "1", true);
            // 同步等待流式输出完成
            codeStream.blockLast(Duration.ofMinutes(10L)); // 等待10分钟
            // 根据类型获取生成的代码目录
            App app = appService.getById(398280987424382976L);
            String generatedCodeDir = String.format("%s/%s_%s/v%s", AppConstant.CODE_OUTPUT_ROOT_DIR,
                    generationType.getValue(), app.getId(), app.getCurrentVersion());
            log.info("AI 代码生成完成，生成目录：{}", generatedCodeDir);
            // 更新状态
            context.setCurrentStep("代码生成");
            context.setGeneratedCodeDir(generatedCodeDir);
            log.info("代码生成完成，目录: {}", generatedCodeDir);
            return WorkflowContext.saveContext(context);
        });
    }

    private static String buildUserMessage(WorkflowContext context) {
        String userMessage = context.getEnhancedPrompt();
        // 检查是否存在质检失败的文件
        QualityResult qualityResult = context.getQualityResult();
        if (isQualityCheckFailed(qualityResult)) {
            // 添加错误修复提示
            userMessage = buildErrorFixPrompt(qualityResult);
        }
        return userMessage;
    }

    private static String buildErrorFixPrompt(QualityResult qualityResult) {
        StringBuilder errorInfo = new StringBuilder();
        errorInfo.append("\n\n## 代码生成过程中存在错误，请修复以下错误：\n");
        for (String error : qualityResult.getErrors()) {
            errorInfo.append("- ").append(error).append("\n");
        }
        // 添加错误修复建议（如果有）
        if (CollUtil.isNotEmpty(qualityResult.getSuggestions())) {
            errorInfo.append("\n## 错误修复建议：\n");
            for (String suggestion : qualityResult.getSuggestions()) {
                errorInfo.append("- ").append(suggestion).append("\n");
            }
        }
        return errorInfo.toString();
    }

    /**
     * 判断质检是否失败
     */
    private static boolean isQualityCheckFailed(QualityResult qualityResult) {
        return qualityResult != null &&
                !qualityResult.getIsValid() &&
                qualityResult.getErrors() != null &&
                !qualityResult.getErrors().isEmpty();
    }
}
