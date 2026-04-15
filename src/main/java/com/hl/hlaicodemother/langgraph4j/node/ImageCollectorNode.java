package com.hl.hlaicodemother.langgraph4j.node;

import com.hl.hlaicodemother.ai.ImageCollectionPlanService;
import com.hl.hlaicodemother.ai.ImageCollectionPlanServiceFactory;
import com.hl.hlaicodemother.langgraph4j.model.ImageCollectionPlan;
import com.hl.hlaicodemother.langgraph4j.model.ImageResource;
import com.hl.hlaicodemother.langgraph4j.state.WorkflowContext;
import com.hl.hlaicodemother.langgraph4j.tools.ImageSearchTool;
import com.hl.hlaicodemother.langgraph4j.tools.LogoGeneratorTool;
import com.hl.hlaicodemother.langgraph4j.tools.MermaidDiagramTool;
import com.hl.hlaicodemother.langgraph4j.tools.UndrawIllustrationTool;
import com.hl.hlaicodemother.utils.SpringContextUtil;
import lombok.extern.slf4j.Slf4j;
import org.bsc.langgraph4j.action.AsyncNodeAction;
import org.bsc.langgraph4j.prebuilt.MessagesState;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

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
            List<ImageResource> collectedImages = new ArrayList<>();
            try {
                // 创建图片收集服务
                ImageCollectionPlanServiceFactory factory =
                        SpringContextUtil.getBean(ImageCollectionPlanServiceFactory.class);
                ImageCollectionPlanService imageCollectionPlanService = factory.createImageCollectionPlanService();
                ImageCollectionPlan plan = imageCollectionPlanService.planImageCollection(originalPrompt);

                // 并发执行图片收集任务
                List<CompletableFuture<List<ImageResource>>> futures = new ArrayList<>();
                // 并发执行内容图片搜索
                if (plan.getContentImageTasks() != null) {
                    ImageSearchTool imageSearchTool = SpringContextUtil.getBean(ImageSearchTool.class);
                    for (ImageCollectionPlan.ImageSearchTask task : plan.getContentImageTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() -> imageSearchTool.searchContentImages(task.query())));
                    }
                }
                // 并发执行插画图片搜索
                if (plan.getIllustrationTasks() != null) {
                    UndrawIllustrationTool undrawIllustrationTool =
                            SpringContextUtil.getBean(UndrawIllustrationTool.class);
                    for (ImageCollectionPlan.IllustrationTask task : plan.getIllustrationTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() -> undrawIllustrationTool.searchIllustrations(task.query())));
                    }
                }
                // 并发执行架构图生成
                if (plan.getDiagramTasks() != null) {
                    // 创建 Mermaid 图工具
                    MermaidDiagramTool mermaidDiagramTool = SpringContextUtil.getBean(MermaidDiagramTool.class);
                    for (ImageCollectionPlan.DiagramTask task : plan.getDiagramTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() -> mermaidDiagramTool.generateMermaidDiagram(task.mermaidCode(), task.description())));
                    }
                }
                // 并发执行 Logo 生成
                if (plan.getLogoTasks() != null) {
                    // 创建 Logo 生成工具
                    LogoGeneratorTool logoGeneratorTool = SpringContextUtil.getBean(LogoGeneratorTool.class);
                    for (ImageCollectionPlan.LogoTask task : plan.getLogoTasks()) {
                        futures.add(CompletableFuture.supplyAsync(() -> logoGeneratorTool.generateLogos(task.description())));
                    }
                }
                // 等待所有任务完成
                CompletableFuture<Void> allTasks = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
                allTasks.join();
                // 获取所有任务的结果
                for (CompletableFuture<List<ImageResource>> future : futures) {
                    List<ImageResource> images = future.get();
                    if (images != null) {
                        collectedImages.addAll(images);
                    }
                }
            } catch (Exception e) {
                log.error("图片收集失败:{}", e.getMessage(), e);
            }
            // 更新状态
            context.setCurrentStep("图片收集");
            context.setImageList(collectedImages);
            return WorkflowContext.saveContext(context);
        });
    }
}
