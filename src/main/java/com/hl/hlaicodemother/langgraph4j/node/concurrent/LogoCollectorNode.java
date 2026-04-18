package com.hl.hlaicodemother.langgraph4j.node.concurrent;

import cn.hutool.core.collection.CollUtil;
import com.hl.hlaicodemother.langgraph4j.model.ImageCollectionPlan;
import com.hl.hlaicodemother.langgraph4j.model.ImageResource;
import com.hl.hlaicodemother.langgraph4j.state.WorkflowContext;
import com.hl.hlaicodemother.langgraph4j.tools.LogoGeneratorTool;
import com.hl.hlaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * Logo 设计节点
 */
@Slf4j
public class LogoCollectorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            ImageCollectionPlan plan = context.getImageCollectionPlan();
            List<ImageResource> logos = new ArrayList<>();
            try {
                if (plan != null && plan.getLogoTasks() != null) {
                    List<ImageCollectionPlan.LogoTask> logoTasks = plan.getLogoTasks();
                    log.info("开始执行 Logo 设计任务，共 {} 个任务", logoTasks.size());
                    LogoGeneratorTool logoGeneratorTool = SpringContextUtil.getBean(LogoGeneratorTool.class);
                    for (ImageCollectionPlan.LogoTask task : logoTasks) {
                        List<ImageResource> imageResources = logoGeneratorTool.generateLogos(task.description());
                        if (CollUtil.isNotEmpty(imageResources)) {
                            logos.addAll(imageResources);
                        }
                    }
                    log.info("Logo 设计完成，共生成 {} 张图片", logos.size());
                }
            } catch (Exception e) {
                log.error("Logo 设计出错：{}", e.getMessage());
            }
            context.setLogos(logos);
            return Map.of(
                    WorkflowContext.WORKFLOW_CONTEXT_KEY, context,
                    WorkflowContext.LOGOS_RESULT_KEY, logos
            );
        });
    }
}
