package com.hl.hlaicodemother.langgraph4j.node.concurrent;

import cn.hutool.core.collection.CollUtil;
import com.hl.hlaicodemother.langgraph4j.model.ImageCollectionPlan;
import com.hl.hlaicodemother.langgraph4j.model.ImageResource;
import com.hl.hlaicodemother.langgraph4j.state.WorkflowContext;
import com.hl.hlaicodemother.langgraph4j.tools.ImageSearchTool;
import com.hl.hlaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 获取内容图片节点
 */
@Slf4j
public class ContentImageCollectorNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            ImageCollectionPlan plan = context.getImageCollectionPlan();
            List<ImageResource> contentImages = new ArrayList<>();
            try {
                if (plan != null && plan.getContentImageTasks() != null) {
                    List<ImageCollectionPlan.ImageSearchTask> contentImageTasks = plan.getContentImageTasks();
                    log.info("开始搜索内容图片，共 {} 个任务", contentImageTasks.size());
                    // 获取工具
                    ImageSearchTool imageSearchTool = SpringContextUtil.getBean(ImageSearchTool.class);
                    for (ImageCollectionPlan.ImageSearchTask task : contentImageTasks) {
                        List<ImageResource> imageResources = imageSearchTool.searchContentImages(task.query());
                        if (CollUtil.isNotEmpty(imageResources)) {
                            contentImages.addAll(imageResources);
                        }
                    }
                    log.info("内容图片收集完成，共收集到 {} 张图片", contentImages.size());
                }
            } catch (Exception e) {
                log.error("获取内容图片出错：{}", e.getMessage());
            }
            context.setContentImages(contentImages);
            return Map.of(
                    WorkflowContext.WORKFLOW_CONTEXT_KEY, context,
                    WorkflowContext.CONTENT_IMAGES_RESULT_KEY, contentImages
            );
        });
    }
}
