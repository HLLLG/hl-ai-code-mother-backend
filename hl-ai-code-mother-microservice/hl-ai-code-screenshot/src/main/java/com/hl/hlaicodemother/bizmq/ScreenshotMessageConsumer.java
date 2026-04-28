package com.hl.hlaicodemother.bizmq;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hl.hlaicodemother.model.ScreenshotTaskMessage;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.exception.ThrowUtils;
import com.hl.hlaicodemother.innerservice.InnerAppService;
import com.hl.hlaicodemother.model.entity.App;
import com.hl.hlaicodemother.service.ScreenshotService;
import com.rabbitmq.client.Channel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 消息消费者
 */
@Component
@Slf4j
public class ScreenshotMessageConsumer {

    @Resource
    private ScreenshotService screenshotService;

    @DubboReference(check = false)
    private InnerAppService appService;

    /**
     * 处理截图任务：单线程消费有利于单例 WebDriver 不并发导航。
     */
    @RabbitListener(queues = ScreenshotMqConstants.QUEUE, ackMode = "MANUAL", concurrency = "1")
    public void receiveScreenshotTask(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        log.info("receive screenshot task: {}", message);
        try {
            ScreenshotTaskMessage task = JSONUtil.toBean(message, ScreenshotTaskMessage.class);
            if (task == null || task.getAppId() == null || StrUtil.isBlank(task.getAppDeployUrl())) {
                log.warn("截图任务消息无效，跳过: {}", message);
                ackQuietly(channel, deliveryTag);
                return;
            }
            String cosUrl = screenshotService.generateAndUploadScreenshot(task.getAppDeployUrl());
            if (StrUtil.isBlank(cosUrl)) {
                log.warn("截图或上传失败，跳过更新封面 appId={}", task.getAppId());
                ackQuietly(channel, deliveryTag);
                return;
            }
            App updateApp = new App();
            updateApp.setCover(cosUrl);
            updateApp.setId(task.getAppId());
            boolean updateResult = appService.updateById(updateApp);
            ThrowUtils.throwIf(!updateResult, ErrorCode.OPERATION_ERROR, "更新应用封面失败");
            ackQuietly(channel, deliveryTag);
        } catch (Exception e) {
            log.error("处理截图消息失败: {}", message, e);
            nackQuietly(channel, deliveryTag);
        }
    }

    private void ackQuietly(Channel channel, long deliveryTag) {
        try {
            channel.basicAck(deliveryTag, false);
        } catch (IOException e) {
            log.error("basicAck 失败, deliveryTag={}", deliveryTag, e);
        }
    }

    private void nackQuietly(Channel channel, long deliveryTag) {
        try {
            // 不重入队，避免坏消息/代码缺陷导致无限重试；需要重试可改为 requeue true 或配合死信队列
            channel.basicNack(deliveryTag, false, false);
        } catch (IOException e) {
            log.error("basicNack 失败, deliveryTag={}", deliveryTag, e);
        }
    }
}
