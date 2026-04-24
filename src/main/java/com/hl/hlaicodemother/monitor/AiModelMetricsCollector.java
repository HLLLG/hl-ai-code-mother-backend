package com.hl.hlaicodemother.monitor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI指标采集器
 */
@Component
public class AiModelMetricsCollector {

    private static final String REQUEST_METRIC_NAME = "ai_mode_request_count";

    private static final String ERROR_METRIC_NAME = "ai_mode_error_count";

    private static final String TOKEN_METRIC_NAME = "ai_mode_token_count";

    private static final String RESPONSE_TIME_METRIC_NAME = "ai_mode_response_duration_seconds";

    private static final String UNKNOWN_TAG_VALUE = "unknown";

    @Resource
    private MeterRegistry meterRegistry;

    // 缓存已创建的指标，避免重复创建（按指标类型分离缓存）
    private final ConcurrentHashMap<String, Counter> requestCountersCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> errorCountersCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Counter> tokenCountersCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Timer> responseTimersCache = new ConcurrentHashMap<>();

    /**
     * 记录请求次数
     */
    public void recordRequest(String userId, String appId, String modelName, String status) {
        String sanitizedUserId = sanitizeTagValue(userId);
        String sanitizedAppId = sanitizeTagValue(appId);
        String sanitizedModelName = sanitizeTagValue(modelName);
        String sanitizedStatus = sanitizeTagValue(status);

        String key = String.join("|", sanitizedUserId, sanitizedAppId, sanitizedModelName, sanitizedStatus);
        Counter counter = requestCountersCache.computeIfAbsent(key, k -> Counter.builder(REQUEST_METRIC_NAME)
                .description("AI模型总请求次数")
                .tag("user_id", sanitizedUserId)
                .tag("app_id", sanitizedAppId)
                .tag("model_name", sanitizedModelName)
                .tag("status", sanitizedStatus)
                .register(meterRegistry));
        counter.increment();
    }

    /**
     * 记录错误次数
     */
    public void recordError(String userId, String appId, String modelName, String errorMessage) {
        String sanitizedUserId = sanitizeTagValue(userId);
        String sanitizedAppId = sanitizeTagValue(appId);
        String sanitizedModelName = sanitizeTagValue(modelName);
        String sanitizedErrorMessage = sanitizeTagValue(errorMessage);
        String key = String.join("|", sanitizedUserId, sanitizedAppId, sanitizedModelName, sanitizedErrorMessage);
        Counter counter = errorCountersCache.computeIfAbsent(key, k -> Counter.builder(ERROR_METRIC_NAME)
                .description("AI模型错误次数")
                .tag("user_id", sanitizedUserId)
                .tag("app_id", sanitizedAppId)
                .tag("model_name", sanitizedModelName)
                .tag("error_message", sanitizedErrorMessage)
                .register(meterRegistry));
        counter.increment();
    }

    /**
     * 记录Token消耗
     */
    public void recordToken(String userId, String appId, String tokenType, String modelName, long tokenCount) {
        if (tokenCount <= 0) {
            return;
        }
        String sanitizedUserId = sanitizeTagValue(userId);
        String sanitizedAppId = sanitizeTagValue(appId);
        String sanitizedTokenType = sanitizeTagValue(tokenType);
        String sanitizedModelName = sanitizeTagValue(modelName);
        String key = String.join("|", sanitizedUserId, sanitizedAppId, sanitizedModelName, sanitizedTokenType);
        Counter counter = tokenCountersCache.computeIfAbsent(key, k -> Counter.builder(TOKEN_METRIC_NAME)
                .description("AI模型Token消耗")
                .tag("user_id", sanitizedUserId)
                .tag("app_id", sanitizedAppId)
                .tag("model_name", sanitizedModelName)
                .tag("token_type", sanitizedTokenType)
                .register(meterRegistry));
        counter.increment(tokenCount);
    }

    /**
     * 响应时间
     */
    public void recordResponseTime(String userId, String appId, String modelName, Duration duration) {
        if (duration == null) {
            return;
        }
        Duration nonNegativeDuration = duration.isNegative() ? Duration.ZERO : duration;
        String sanitizedUserId = sanitizeTagValue(userId);
        String sanitizedAppId = sanitizeTagValue(appId);
        String sanitizedModelName = sanitizeTagValue(modelName);
        String key = String.join("|", sanitizedUserId, sanitizedAppId, sanitizedModelName);
        Timer timer = responseTimersCache.computeIfAbsent(key, k -> Timer.builder(RESPONSE_TIME_METRIC_NAME)
                .description("AI模型响应时间")
                .tag("user_id", sanitizedUserId)
                .tag("app_id", sanitizedAppId)
                .tag("model_name", sanitizedModelName)
                .register(meterRegistry));
        timer.record(nonNegativeDuration);
    }

    private String sanitizeTagValue(String value) {
        if (value == null || value.isBlank()) {
            return UNKNOWN_TAG_VALUE;
        }
        return value;
    }
}
