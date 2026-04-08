package com.hl.hlaicodemother.langgraph4j.node;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.hl.hlaicodemother.langgraph4j.model.ImageResource;
import com.hl.hlaicodemother.langgraph4j.state.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.List;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 提示词增强节点
 */
@Slf4j
public class PromptEnhancerNode {
    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 提示词增强");
            // 获取原始提示词
            String originalPrompt = context.getOriginalPrompt();
            StringBuilder enhancedPrompt = new StringBuilder();
            enhancedPrompt.append(originalPrompt);
            String imageListStr = context.getImageListStr();
            List<ImageResource> imageList = context.getImageList();
            if (StrUtil.isNotBlank(imageListStr) || CollUtil.isNotEmpty(imageList)) {
                enhancedPrompt.append("\n\n## 可用素材资源\n");
                enhancedPrompt.append("请在生成网站使用以下图片资源，将这些图片合理地嵌入到网站的相应位置中。\n");
                if (CollUtil.isNotEmpty(imageList)) {
                    for (ImageResource imageResource : imageList) {
                        enhancedPrompt.append("- ")
                                .append(imageResource.getCategory().getValue())
                                .append(": ")
                                .append(imageResource.getDescription())
                                .append("(")
                                .append(imageResource.getUrl())
                                .append(")");
                    }
                } else {
                    enhancedPrompt.append(imageListStr);
                }
            }
            // 更新状态
            context.setCurrentStep("提示词增强");
            context.setEnhancedPrompt(enhancedPrompt.toString());
            log.info("提示词增强完成，增强后长度：{} 字符", enhancedPrompt.length());
            return WorkflowContext.saveContext(context);
        });
    }
}
