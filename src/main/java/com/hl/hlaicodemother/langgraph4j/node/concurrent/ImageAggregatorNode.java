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

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            List<ImageResource> images = new ArrayList<>();
            log.info("开始聚合并发收集的图片");
            // 从各个中间字段聚合图片
            if (context.getContentImages() != null) {
                images.addAll(context.getContentImages());
            }
            if (context.getIllustrations() != null) {
                images.addAll(context.getIllustrations());
            }
            if (context.getDiagrams() != null) {
                images.addAll(context.getDiagrams());
            }
            if (context.getLogos() != null) {
                images.addAll(context.getLogos());
            }
            log.info("图片聚合完成，共 {} 张图片", images.size());
            context.setImageList(images);
            context.setCurrentStep("图片聚合");
            return WorkflowContext.saveContext(context);
        });
    }
}
