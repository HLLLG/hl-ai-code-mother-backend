package com.hl.hlaicodemother.langgraph4j.node.concurrent;

import com.hl.hlaicodemother.langgraph4j.model.ImageResource;
import com.hl.hlaicodemother.langgraph4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 图片聚合节点
 */
@Slf4j
public class ImageAggregatorNode {

    /**
     * 添加非空的图片列表。
     */
    private static void addIfPresent(List<ImageResource> target, List<ImageResource> part) {
        if (part != null) {
            target.addAll(part);
        }
    }

    /** 优先使用并发节点写入的独立 key，否则回退到 WorkflowContext 中的字段（兼容旧路径或单测）。 */
    private static List<ImageResource> coalesce(
            MessagesState<String> state,
            String resultKey,
            List<ImageResource> contextFallback) {
        List<ImageResource> fromState = WorkflowContext.getConcurrentImageResult(state, resultKey);
        return fromState != null ? fromState : contextFallback;
    }

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            List<ImageResource> images = new ArrayList<>();
            log.info("开始聚合并发收集的图片");
            List<ImageResource> content = coalesce(state,  WorkflowContext.CONTENT_IMAGES_RESULT_KEY, context.getContentImages());
            List<ImageResource> illustrations =
                    coalesce(state, WorkflowContext.ILLUSTRATIONS_RESULT_KEY, context.getIllustrations());
            List<ImageResource> diagrams = coalesce(state, WorkflowContext.DIAGRAMS_RESULT_KEY, context.getDiagrams());
            List<ImageResource> logos = coalesce(state, WorkflowContext.LOGOS_RESULT_KEY, context.getLogos());
            addIfPresent(images, content);
            addIfPresent(images, illustrations);
            addIfPresent(images, diagrams);
            addIfPresent(images, logos);
            log.info("图片聚合完成，共 {} 张图片", images.size());
            context.setImageList(images);
            context.setCurrentStep("图片聚合");
            return WorkflowContext.saveContext(context);
        });
    }
}
