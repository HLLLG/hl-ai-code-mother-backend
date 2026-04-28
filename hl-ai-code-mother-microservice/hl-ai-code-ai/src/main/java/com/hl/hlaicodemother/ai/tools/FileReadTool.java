package com.hl.hlaicodemother.ai.tools;

import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件读取工具，通过构造函数绑定 appId，与 {@link FileWriteTool} 共用同一套输出目录
 * （vue_project_{appId}/v{n}/），须在每轮生成前通过 {@link #prepareForGeneration(int)} 写入版本号。
 */
@Slf4j
public class FileReadTool extends BaseTool {

    public FileReadTool(Long appId) {
        super(appId);
    }

    @Tool("读取指定路径的文件内容")
    public String readFile(@P("文件的相对路径") String relativeFilePath) {
        if (cancelled.get()) {
            throw new RuntimeException("生成任务已取消，停止文件读取");
        }
        try {
            Path path = Paths.get(relativeFilePath);
            if (!path.isAbsolute()) {
                path = this.resolveProjectPath(relativeFilePath);
            }
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return "错误：文件不存在或不是文件 - " + relativeFilePath;
            }
            return Files.readString(path);
        } catch (IOException e) {
            String errorMessage = "读取文件失败: " + relativeFilePath + ", 错误: " + e.getMessage();
            log.error(errorMessage, e);
            return errorMessage;
        }
    }

    @Override
    public String getToolName() {
        return "readFile";
    }

    @Override
    public String getDisplayName() {
        return "读取文件";
    }

    @Override
    public String generateToolExecuteResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        return String.format("[工具调用] %s：%s", getDisplayName(), relativeFilePath);
    }
}
