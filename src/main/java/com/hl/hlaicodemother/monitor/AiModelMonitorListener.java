package com.hl.hlaicodemother.monitor;

import dev.langchain4j.model.chat.listener.ChatModelErrorContext;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.listener.ChatModelRequestContext;
import dev.langchain4j.model.chat.listener.ChatModelResponseContext;
import dev.langchain4j.model.output.TokenUsage;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

@Component
@Slf4j
public class AiModelMonitorListener implements ChatModelListener {

    // 请求开始时间的 key
    private static final String REQUEST_START_TIME_KEY = "requestStartTime";

    // 监控上下文 key
    private static final String MONITOR_CONTEXT_KEY = "monitorContext";

    private static final String UNKNOWN_USER_ID = "unknown_user";

    private static final String UNKNOWN_APP_ID = "unknown_app";

    private static final String UNKNOWN_MODEL_NAME = "unknown_model";

    private static final String UNKNOWN_ERROR_MESSAGE = "unknown_error";

    @Resource
    private AiModelMetricsCollector aiModelMetricsCollector;

    @Override
    public void onRequest(ChatModelRequestContext requestContext) {
        // 记录请求开始时间
        requestContext.attributes().put(REQUEST_START_TIME_KEY, Instant.now());
        MonitorContext monitorContext = normalizeMonitorContext(MonitorContextHolder.get());
        requestContext.attributes().put(MONITOR_CONTEXT_KEY, monitorContext);
        String modelName = normalizeTagValue(requestContext.chatRequest().modelName(), UNKNOWN_MODEL_NAME);
        // 记录请求次数
        aiModelMetricsCollector.recordRequest(
                monitorContext.getUserId(),
                monitorContext.getAppId(),
                modelName,
                "started"
        );

    }

    @Override
    public void onResponse(ChatModelResponseContext responseContext) {
        // 获取模型名称
        String modelName = normalizeTagValue(responseContext.chatRequest().modelName(), UNKNOWN_MODEL_NAME);
        Map<Object, Object> attributes = responseContext.attributes();
        MonitorContext monitorContext = resolveMonitorContext(attributes);
        String userId = monitorContext.getUserId();
        String appId = monitorContext.getAppId();
        // 记录响应时间
        recordResponseTime(attributes, modelName, userId, appId);
        // 记录Token消耗
        recordTokenUsage(responseContext, modelName, userId, appId);
    }

    @Override
    public void onError(ChatModelErrorContext errorContext) {
        Map<Object, Object> attributes = errorContext.attributes();
        MonitorContext monitorContext = resolveMonitorContext(attributes);
        String appId = monitorContext.getAppId();
        String userId = monitorContext.getUserId();
        String modelName = normalizeTagValue(errorContext.chatRequest().modelName(), UNKNOWN_MODEL_NAME);
        String errorMessage = extractErrorMessage(errorContext);
        // 记录错误请求
        aiModelMetricsCollector.recordRequest(userId, appId, modelName, "error");
        aiModelMetricsCollector.recordError(userId, appId, modelName, errorMessage);
        // 记录响应时间
        recordResponseTime(attributes, modelName, userId, appId);
    }

    private void recordResponseTime(Map<Object, Object> attributes, String modelName, String userId,
                               String appId) {
        // 获取请求开始时间
        Instant requestStartTime = (Instant) attributes.get(REQUEST_START_TIME_KEY);
        if (requestStartTime == null) {
            log.debug("recordResponseTime skipped because request start time is missing");
            return;
        }
        // 计算响应时间
        Duration responseTime = Duration.between(requestStartTime, Instant.now());
        if (responseTime.isNegative()) {
            responseTime = Duration.ZERO;
        }
        aiModelMetricsCollector.recordResponseTime(userId, appId, modelName, responseTime);
    }


    private void recordTokenUsage(ChatModelResponseContext responseContext, String modelName, String userId, String appId) {
        TokenUsage tokenUsage = responseContext.chatResponse().metadata().tokenUsage();
        if (tokenUsage == null) {
            return;
        }
        aiModelMetricsCollector.recordToken(userId, appId, "input", modelName, tokenUsage.inputTokenCount());
        aiModelMetricsCollector.recordToken(userId, appId, "output", modelName, tokenUsage.outputTokenCount());
        aiModelMetricsCollector.recordToken(userId, appId, "total", modelName, tokenUsage.totalTokenCount());
    }

    private MonitorContext resolveMonitorContext(Map<Object, Object> attributes) {
        Object contextObj = attributes.get(MONITOR_CONTEXT_KEY);
        if (contextObj instanceof MonitorContext context) {
            return normalizeMonitorContext(context);
        }
        // 兜底: 避免异步线程导致 attributes 中未携带上下文
        return normalizeMonitorContext(MonitorContextHolder.get());
    }

    private MonitorContext normalizeMonitorContext(MonitorContext context) {
        String userId = context == null ? null : context.getUserId();
        String appId = context == null ? null : context.getAppId();
        return MonitorContext.builder()
                .userId(normalizeTagValue(userId, UNKNOWN_USER_ID))
                .appId(normalizeTagValue(appId, UNKNOWN_APP_ID))
                .build();
    }

    private String extractErrorMessage(ChatModelErrorContext errorContext) {
        Throwable error = errorContext.error();
        if (error == null) {
            return UNKNOWN_ERROR_MESSAGE;
        }
        String message = normalizeTagValue(error.getMessage(), error.getClass().getSimpleName());
        return normalizeTagValue(message, UNKNOWN_ERROR_MESSAGE);
    }

    private String normalizeTagValue(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        return value;
    }
}
