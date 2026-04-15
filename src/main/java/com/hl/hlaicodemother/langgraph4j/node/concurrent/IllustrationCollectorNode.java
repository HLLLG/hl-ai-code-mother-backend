package com.hl.hlaicodemother.langgraph4j.node.concurrent;

import cn.hutool.core.collection.CollUtil;
import com.hl.hlaicodemother.langgraph4j.model.ImageCollectionPlan;
import com.hl.hlaicodemother.langgraph4j.model.ImageResource;
import com.hl.hlaicodemother.langgraph4j.state.WorkflowContext;
import com.hl.hlaicodemother.langgraph4j.tools.UndrawIllustrationTool;
import com.hl.hlaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 插画搜索节点
 */
@Slf4j
public class IllustrationCollectorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            ImageCollectionPlan plan = context.getImageCollectionPlan();
            List<ImageResource> illustrations = new ArrayList<>();
            try {
                if (plan != null && plan.getIllustrationTasks() != null) {
                    List<ImageCollectionPlan.IllustrationTask> illustrationTasks = plan.getIllustrationTasks();
                    log.info("开始搜索插画，任务数：{}", illustrationTasks.size());
                    UndrawIllustrationTool undrawIllustrationTool =
                            SpringContextUtil.getBean(UndrawIllustrationTool.class);
                    for (ImageCollectionPlan.IllustrationTask task : illustrationTasks) {
                        List<ImageResource> imageResources = undrawIllustrationTool.searchIllustrations(task.query());
                        if (CollUtil.isNotEmpty(imageResources)) {
                            illustrations.addAll(imageResources);
                        }
                    }
                    log.info("插画搜索完成，共收集到 {} 张图片", illustrations.size());
                }

            } catch (Exception e) {
                log.error("插画搜索出错：{}", e.getMessage());
            }
            context.setIllustrations(illustrations);
            context.setCurrentStep("插画搜索");
            return WorkflowContext.saveContext(context);
        });
    }
}
