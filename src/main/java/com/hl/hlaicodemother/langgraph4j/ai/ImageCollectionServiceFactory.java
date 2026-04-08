package com.hl.hlaicodemother.langgraph4j.ai;

import com.hl.hlaicodemother.langgraph4j.tools.ImageSearchTool;
import com.hl.hlaicodemother.langgraph4j.tools.LogoGeneratorTool;
import com.hl.hlaicodemother.langgraph4j.tools.MermaidDiagramTool;
import com.hl.hlaicodemother.langgraph4j.tools.UndrawIllustrationTool;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.service.AiServices;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * 图片收集服务工厂
 *
 * @author hl
 */
@Component
public class ImageCollectionServiceFactory {

    @Resource
    private ChatModel chatModel;

    @Resource
    private ImageSearchTool imageSearchTool;

    @Resource
    private LogoGeneratorTool logoGeneratorTool;

    @Resource
    private MermaidDiagramTool mermaidDiagramTool;

    @Resource
    private UndrawIllustrationTool undrawIllustrationTool;

    @Bean
    public ImageCollectionService imageCollectionService() {
        return AiServices.builder(ImageCollectionService.class)
                .chatModel(chatModel)
                .tools(imageSearchTool, logoGeneratorTool, mermaidDiagramTool, undrawIllustrationTool)
                .build();
    }
}
