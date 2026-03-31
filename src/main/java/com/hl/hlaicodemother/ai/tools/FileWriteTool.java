package com.hl.hlaicodemother.ai.tools;

import cn.hutool.core.io.FileUtil;
import com.hl.hlaicodemother.constant.AppConstant;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.MemoryId;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 文件写入工具类
 */
@Slf4j
public class FileWriteTool {

   /**
     * 写入文件
     *
     * @param relativeFilePath 文件的相对路径或绝对路径
     * @param content          要写入的文件内容
     * @param appId            应用 ID
     * @return 操作结果消息
     */
    @Tool("写入文件到指定路径")
    public static String writeFile(@P("文件的相对路径") String relativeFilePath, @P("要写入的文件内容") String content,
                                   @MemoryId Long appId) {
        try {
            // 将相对路径或绝对路径转换为 Path 对象
            String pathStr = relativeFilePath;
            if (!Paths.get(pathStr).isAbsolute()) {
                String projectDirName = "vue_project_" + appId;
                String projectRoot = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + projectDirName;
                pathStr = FileUtil.normalize(projectRoot + File.separator + relativeFilePath);
            }
            Path path = Paths.get(pathStr);
            // 自动创建父目录（如果不存在）
            FileUtil.mkdir(path.getParent().toString());
            // 写入文件
            FileUtil.writeUtf8String(content, path.toString());
            log.info("文件写入成功：{}", path.toAbsolutePath());
            // 返回操作结果消息，包含原始相对路径
            return "文件写入成功：" + relativeFilePath;
        } catch (Exception e) {
            String errorMessage = "写入文件失败：" + relativeFilePath + "，错误信息：" + e.getMessage();
            log.error("写入文件失败：{}", e.getMessage());
            return errorMessage;
        }
    }
}
