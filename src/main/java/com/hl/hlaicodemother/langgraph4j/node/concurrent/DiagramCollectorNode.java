package com.hl.hlaicodemother.langgraph4j.node.concurrent;

import cn.hutool.core.collection.CollUtil;
import com.hl.hlaicodemother.langgraph4j.model.ImageCollectionPlan;
import com.hl.hlaicodemother.langgraph4j.model.ImageResource;
import com.hl.hlaicodemother.langgraph4j.state.WorkflowContext;
import com.hl.hlaicodemother.langgraph4j.tools.MermaidDiagramTool;
import com.hl.hlaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Mermaid 架构图生成节点
 */
@Slf4j
public class DiagramCollectorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            ImageCollectionPlan plan = context.getImageCollectionPlan();
            List<ImageResource> diagrams = new ArrayList<>();
            try {
                if (plan != null && plan.getDiagramTasks() != null) {
                    List<ImageCollectionPlan.DiagramTask> diagramTasks = plan.getDiagramTasks();
                    log.info("开始生成 Mermaid 架构图，共 {} 个任务", diagramTasks.size());
                    MermaidDiagramTool mermaidDiagramTool = SpringContextUtil.getBean(MermaidDiagramTool.class);
                    for (ImageCollectionPlan.DiagramTask task : diagramTasks) {
                        List<ImageResource> imageResources = mermaidDiagramTool.generateMermaidDiagram(
                                task.mermaidCode(), task.description());
                        if (CollUtil.isNotEmpty(imageResources)) {
                            diagrams.addAll(imageResources);
                        }
                    }
                    log.info("Mermaid 架构图生成完成，共生成 {} 张图片", diagrams.size());
                }
            } catch (Exception e) {
                log.error("Mermaid 架构图生成出错：{}", e.getMessage());
            }
            context.setDiagrams(diagrams);
            context.setCurrentStep("Mermaid 架构图生成");
            return WorkflowContext.saveContext(context);
        });
    }
}
