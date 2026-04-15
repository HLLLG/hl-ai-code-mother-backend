package com.hl.hlaicodemother.langgraph4j.node.concurrent;

import com.hl.hlaicodemother.ai.ImageCollectionPlanService;
import com.hl.hlaicodemother.ai.ImageCollectionPlanServiceFactory;
import com.hl.hlaicodemother.langgraph4j.model.ImageCollectionPlan;
import com.hl.hlaicodemother.langgraph4j.state.WorkflowContext;
import com.hl.hlaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

/**
 * 图片规划节点
 */
@Slf4j
public class ImagePlanNode {

    public static AsyncNodeAction<MessagesState<String>> create() {
        return node_async(state -> {
            WorkflowContext context = WorkflowContext.getContext(state);
            log.info("执行节点: 图片规划");
            // 获取用户输入的提示词
            String prompt = context.getOriginalPrompt();
            try {
                // 创建图片规划服务
                ImageCollectionPlanServiceFactory factory = SpringContextUtil.getBean(ImageCollectionPlanServiceFactory.class);
                ImageCollectionPlanService imageCollectionPlanService = factory.createImageCollectionPlanService();
                ImageCollectionPlan plan = imageCollectionPlanService.planImageCollection(prompt);
                log.info("图片规划完成：{}, 准备启动并发分支", plan);
                // 更新状态
                context.setCurrentStep("图片规划");
                context.setImageCollectionPlan(plan);
            } catch (Exception e) {
                log.error("图片规划出错：{}", e.getMessage());
            }
            return WorkflowContext.saveContext(context);
        });
    }
}
