package com.hl.hlaicodemother.ai.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件写入工具类，通过构造函数绑定 appId，避免将 appId 暴露为工具参数
 */
@Slf4j
public class FileWriteTool extends BaseTool {

    private final Set<String> writtenFiles = new HashSet<>();
    private static final int MAX_FILE_COUNT = 30;
    private static final int MAX_REJECT_COUNT = 3;
    private int consecutiveRejectCount = 0;

    public FileWriteTool(Long appId) {
        super(appId);
    }

    /**
     * 新一轮生成前调用：清空已写文件记录、设置本次版本号（目录为 vue_project_{appId}/v{n}/）
     */
    @Override
    public void prepareForGeneration(int version) {
        super.prepareForGeneration(version);
        writtenFiles.clear();
        consecutiveRejectCount = 0;
    }


    /**
     * 写入文件
     *
     * @param relativeFilePath 文件的相对路径或绝对路径
     * @param content          要写入的文件内容
     * @return 操作结果消息
     */
    @Tool("写入文件到指定路径。注意：每个文件只能写入一次，重复写入会返回错误。")
    public String writeFile(@P("文件的相对路径") String relativeFilePath, @P("要写入的文件内容") String content) {
        if (cancelled.get()) {
            throw new RuntimeException("生成任务已取消，停止文件写入");
        }
        // 去重检查
        if (writtenFiles.contains(relativeFilePath)) {
            consecutiveRejectCount++;
            if (consecutiveRejectCount >= MAX_REJECT_COUNT) {
                throw new RuntimeException("工具调用异常：连续 " + MAX_REJECT_COUNT
                        + " 次尝试写入已存在的文件，强制终止生成流程");
            }
            return "错误：文件【" + relativeFilePath + "】已经成功写入过，禁止重复写入同一文件。"
                    + "请继续创建下一个尚未创建的文件，如果所有文件都已创建完毕，请直接输出生成完毕的总结，不要再调用任何工具。";
        }
        // 数量上限检查
        if (writtenFiles.size() >= MAX_FILE_COUNT) {
            consecutiveRejectCount++;
            if (consecutiveRejectCount >= MAX_REJECT_COUNT) {
                throw new RuntimeException("工具调用异常：文件数量已达上限且连续 " + MAX_REJECT_COUNT
                        + " 次被拒绝，强制终止生成流程");
            }
            return "错误：已创建 " + writtenFiles.size() + " 个文件，达到上限。"
                    + "请停止调用工具，直接输出生成完毕的总结。";
        }
        // 本次写入成功，重置连续拒绝计数
        consecutiveRejectCount = 0;
        try {
            Path path = Paths.get(relativeFilePath);
            if (!path.isAbsolute()) {
                path = this.resolveProjectPath(relativeFilePath);
            }
            FileUtil.mkdir(path.getParent().toString());
            FileUtil.writeUtf8String(content, path.toString());
            writtenFiles.add(relativeFilePath);
            log.info("文件写入成功（{}/{}）：{}", writtenFiles.size(), MAX_FILE_COUNT, path.toAbsolutePath());
            return "文件写入成功（" + writtenFiles.size() + "/" + MAX_FILE_COUNT + "）：" + relativeFilePath;
        } catch (Exception e) {
            log.error("写入文件失败：{}", e.getMessage());
            return "写入文件失败：" + relativeFilePath + "，错误信息：" + e.getMessage();
        }
    }

    @Override
    public String getToolName() {
        return "writeFile";
    }

    @Override
    public String getDisplayName() {
        return "写入文件";
    }

    @Override
    public String generateToolExecuteResult(JSONObject arguments) {
        String relativeFilePath = arguments.getStr("relativeFilePath");
        String suffix = FileUtil.getSuffix(relativeFilePath);
        String content = arguments.getStr("content");
        return String.format("""
                        [🔧工具调用] %s %s
                        ```%s
                        %s
                        ```
                        """, getDisplayName(), relativeFilePath, suffix, content);
    }
}
