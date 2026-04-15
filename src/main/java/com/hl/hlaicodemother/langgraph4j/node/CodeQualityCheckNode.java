package com.hl.hlaicodemother.langgraph4j.node;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.hl.hlaicodemother.CodeQualityCheckServiceFactory;
import com.hl.hlaicodemother.ai.CodeQualityCheckService;
import com.hl.hlaicodemother.langgraph4j.model.QualityResult;
import com.hl.hlaicodemother.langgraph4j.state.WorkflowContext;
import com.hl.hlaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 代码质量检查节点
 */
@Slf4j
public class CodeQualityCheckNode {


    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 代码质量检查");

            String generatedCodeDir = context.getGeneratedCodeDir();
            QualityResult qualityResult = null;
            try {
                // 读取代码
                String codeContent = readAndConcatCodeFiles(generatedCodeDir);
                if (StrUtil.isBlank(codeContent)) {
                    log.warn("代码目录为空：{}", generatedCodeDir);
                    qualityResult = QualityResult.builder()
                            .isValid(false)
                            .errors(List.of("代码目录为空"))
                            .suggestions(List.of("请检查代码目录，确保代码文件格式正确"))
                            .build();
                } else {
                    CodeQualityCheckServiceFactory codeQualityCheckServiceFactory =
                            SpringContextUtil.getBean(CodeQualityCheckServiceFactory.class);
                    CodeQualityCheckService codeQualityCheckService = codeQualityCheckServiceFactory.createCodeQualityCheckService();
                    // 检查代码质量
                    qualityResult = codeQualityCheckService.checkCodeQuality(codeContent);
                    log.info("代码质量检查完成 - 是否通过 {}", qualityResult.getIsValid());
                }
            } catch (Exception e) {
                log.error("代码质量检查出错：{}", e.getMessage());
                qualityResult = QualityResult.builder()
                        .isValid(true) // 异常直接跳到下一步骤
                        .build();
            }
            // 更新状态
            context.setCurrentStep("代码质量检查");
            context.setQualityResult(qualityResult);
            return WorkflowContext.saveContext(context);
        });
    }

    /**
     * 需要检查的文件扩展名
     */
    private static final List<String> CODE_EXTENSIONS = Arrays.asList("html", "css", "htm", "js", "json", "vue"
            , "ts", "jsx", "tsx");

    /**
     * 读取并拼接代码目录下的所有代码文件
     */
    private static String readAndConcatCodeFiles(String codeDir) {
        if (StrUtil.isBlank(codeDir)) {
            return "";
        }
        File directory = new File(codeDir);
        if (!directory.exists() || !directory.isDirectory()) {
            log.error("代码目录不存在或无效：{}", codeDir);
            return "";
        }
        StringBuilder codeBuilder = new StringBuilder();
        codeBuilder.append("# 项目文件结构和代码内容\n\n");
        // 使用 Hutool 的 walkFiles 方法遍历文件
        FileUtil.walkFiles(directory, file -> {
            // 跳过隐藏文件
            if (shouldSkipFile(file, directory)) {
                return;
            }
            if (isCodeFile(file)) {
                String relativePath = FileUtil.subPath(directory.getAbsolutePath(), file.getAbsolutePath());
                codeBuilder.append("## 文件：").append(relativePath).append("\n\n");
                codeBuilder.append(FileUtil.readUtf8String(file)).append("\n\n");
            }
        });
        return codeBuilder.toString();
    }

    /**
     * 判断文件是否应该被跳过
     */
    private static boolean shouldSkipFile(File file, File directory) {
        String relativePath = FileUtil.subPath(directory.getAbsolutePath(), file.getAbsolutePath());
        // 跳过隐藏文件
        if (relativePath.startsWith(".") || file.isHidden()) {
            return true;
        }
        // 跳过特定目录下的文件
        return relativePath.contains("node_modules" + File.separator)
                || relativePath.contains("dist" + File.separator)
                || relativePath.contains("target" + File.separator)
                || relativePath.contains(".git" + File.separator);
    }

    /**
     * 判断是否需要检查的代码文件
     */
    private static boolean isCodeFile(File file) {
        String extension = FileUtil.extName(file);
        return CODE_EXTENSIONS.contains(extension);
    }
}
