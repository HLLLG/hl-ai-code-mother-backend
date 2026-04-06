package com.hl.hlaicodemother.ai.tools;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import com.hl.hlaicodemother.constant.AppConstant;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 工具基类
 */
@Data
public abstract class BaseTool {

    protected Long appId;

    public BaseTool(Long appId) {
        this.appId = appId;
    }

    /** 未设置时为 0，须在每轮生成前通过 {@link #prepareForGeneration(int)} 写入 */
    protected AtomicInteger version = new AtomicInteger(0);
    protected AtomicBoolean cancelled = new AtomicBoolean(false);

    /**
     * 新一轮生成前：写入版本号并清空写入工具的本轮状态。
     */
    public void prepareForGeneration(int version) {
        this.version.set(version);
    }

    public void setCancelled(boolean value) {
        cancelled.set(value);
    }

    /**
     * 获取工具名称
     *
     * @return
     */
    public abstract String getToolName();

    /**
     * 获取工具描述
     *
     * @return
     */
    public abstract String getDisplayName();

    /**
     * 生成工具请求时的返回值（显示给用户）
     *
     * @return
     */
    public String generateToolRequestResponse() {
        return String.format("\n\n[选择工具] %s\n\n", getDisplayName());
    }

    /**
     * 生成工具执行时的返回值（显示给用户）
     *
     * @param arguments
     * @return
     */
    public abstract String generateToolExecuteResult(JSONObject arguments);

    protected Path resolveProjectPath(String relativePath) {
        int ver = version.get();
        if (ver <= 0) {
            throw new RuntimeException("内部错误：未设置 Vue 工程输出版本号，无法删除文件");
        }
        String projectRoot = AppConstant.CODE_OUTPUT_ROOT_DIR + File.separator + "vue_project_" + appId
                + File.separator + "v" + ver;
        return Paths.get(FileUtil.normalize(projectRoot + File.separator + relativePath));
    }
}
