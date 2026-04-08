package com.hl.hlaicodemother.langgraph4j.tools;

import cn.hutool.core.util.StrUtil;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGeneration;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationMessage;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationParam;
import com.alibaba.dashscope.aigc.imagegeneration.ImageGenerationResult;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesis;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisParam;
import com.alibaba.dashscope.aigc.imagesynthesis.ImageSynthesisResult;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.utils.Constants;
import com.hl.hlaicodemother.langgraph4j.model.ImageResource;
import com.hl.hlaicodemother.langgraph4j.model.enums.ImageCategoryEnum;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Logo 设计工具
 *
 * @author hl
 */
@Slf4j
@Component
public class LogoGeneratorTool {

    static {
        // 以下为北京地域url，各地域的base_url不同
        Constants.baseHttpApiUrl = "https://dashscope.aliyuncs.com/api/v1";
    }

    @Value("${dashscope.api-key}")
    private String apiKey;

    @Value("${dashscope.image-model}")
    private String model;

    @Tool("根据描述生成 Logo 设计图片，用于网站品牌标识")
    public List<ImageResource> generateLogos(@P("Logo 设计描述，如名称、行业、风格等，尽量详细") String description) {
        List<ImageResource> imageList = new ArrayList<>();

        // 构建Logo 设计提示词
        ImageGenerationMessage message = ImageGenerationMessage.builder()
                .role("user")
                .content(Collections.singletonList(
                        Collections.singletonMap("text", String.format("生成 Logo， Logo中禁止包含任何文字！Logo 介绍 %s", description))
                )).build();

        ImageGenerationParam param = ImageGenerationParam.builder()
                .apiKey(apiKey)
                .model(model)
                .messages(Collections.singletonList(message))
                .enableSequential(false)
                .n(1)
                .size("1280*1280")
                .build();

        ImageGeneration imageGeneration = new ImageGeneration();
        ImageGenerationResult taskResult = null;
        try {
            taskResult = imageGeneration.asyncCall(param);
            // 等待任务完成
            String taskId = taskResult.getOutput().getTaskId();
            ImageGenerationResult result = waitTask(taskId);
            if (result != null && result.getOutput() != null && result.getOutput().getChoices() != null) {
                // 获取图片 URL
                String imageUrl = result.getOutput().getChoices().getFirst().getMessage().getContent().getFirst().get(
                        "image").toString();
                if (StrUtil.isNotBlank(imageUrl)) {
                    imageList.add(ImageResource.builder()
                            .category(ImageCategoryEnum.LOGO)
                            .description(description)
                            .url(imageUrl)
                            .build());
                }
            }
        } catch (Exception e) {
            log.error("生成 Logo 失败：{}", e.getMessage(), e);
        }
        return imageList;
    }

    /**
     * 等待任务完成
     */
    public ImageGenerationResult waitTask(String taskId)
            throws ApiException, NoApiKeyException {
        ImageGeneration imageGeneration = new ImageGeneration();
        return imageGeneration.wait(taskId, apiKey);
    }
}