package com.hl.hlaicodemother.ai.tools;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

/**
 * Vue 工程生成用到的文件类工具工厂类；
 * 由 {@link com.hl.hlaicodemother.ai.AiCodeGeneratorServiceFactory} 按 appId 缓存一份。
 */
@Slf4j
public final class ToolManager {

    /**
     * 工具名称到工具实例的映射
     */
    private final  Map<String, BaseTool> toolMap = new HashMap<>();

    private BaseTool[] tools;

    /**
     * 创建 Vue 工具管理器实例
     */
    public static ToolManager build(Long appId) {
        ToolManager toolManager = new ToolManager();
        toolManager.tools = toolManager.initTools(appId);
        return toolManager;
    }

    /**
     * 初始化工具
     */
    private BaseTool[] initTools(Long appId) {
        // 创建工具实例
        BaseTool[] newTools = new BaseTool[]{
                new FileDeleteTool(appId),
                new FileWriteTool(appId),
                new FileModifyTool(appId),
                new FileDirReadTool(appId),
                new FileReadTool(appId),
                new ExitTool(appId)
        };
        // 初始化工具映射
        for (BaseTool tool : newTools) {
            toolMap.put(tool.getToolName(), tool);
        }
        return newTools;
    }

    /**
     * 获取工具实例
     */
    public BaseTool getTool(String toolName) {
        return toolMap.get(toolName);
    }

    /**
     * 获取所有工具
     */
    public BaseTool[] getAllTools() {
        return tools;
    }

    /**
     * 新一轮生成前：写入版本号并清空写入工具的本轮状态。
     */
    public void prepareForGeneration(int version) {
        for (BaseTool tool : tools) {
            tool.prepareForGeneration(version);
        }
    }

    public void setCancelled(boolean cancelled) {
        for (BaseTool tool : tools) {
            tool.setCancelled(cancelled);
        }
    }
}
