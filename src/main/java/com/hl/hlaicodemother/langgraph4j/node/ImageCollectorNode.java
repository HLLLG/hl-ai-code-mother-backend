package com.hl.hlaicodemother.langgraph4j.node;

import com.hl.hlaicodemother.langgraph4j.ai.ImageCollectionService;
import com.hl.hlaicodemother.langgraph4j.model.ImageResource;
import com.hl.hlaicodemother.langgraph4j.model.enums.ImageCategoryEnum;
import com.hl.hlaicodemother.langgraph4j.state.WorkflowContext;
import com.hl.hlaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.Arrays;
import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 图片收集节点
 *
 * @author hl
 */
@Slf4j
public class ImageCollectorNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 图片收集");
            // 获取原始提示词
            String originalPrompt = context.getOriginalPrompt();
            String imageListStr = null;
            try {
                // 获取AI图片收集服务
                ImageCollectionService imageCollectionService = SpringContextUtil.getBean(ImageCollectionService.class);
                // 调用图片收集服务
                imageListStr = imageCollectionService.collectImages(originalPrompt);
            } catch (Exception e) {
                log.error("图片收集失败:{}", e.getMessage(), e);
            }
            // 更新状态
            context.setCurrentStep("图片收集");
            context.setImageListStr(imageListStr);
            return WorkflowContext.saveContext(context);
        });
    }
}
