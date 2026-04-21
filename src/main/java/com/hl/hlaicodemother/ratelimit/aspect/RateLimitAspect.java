package com.hl.hlaicodemother.ratelimit.aspect;

import cn.hutool.extra.servlet.ServletUtil;
import com.hl.hlaicodemother.exception.BusinessException;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.ratelimit.anotation.RateLimit;
import com.hl.hlaicodemother.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RRateLimiter;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.Duration;

@Component
@Slf4j
@Aspect
public class RateLimitAspect {

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private UserService userService;

    /**
     * 限流切面
     *
     * @param point       切点
     * @param rateLimit 限流注解
     */
    @Before("@annotation(rateLimit)")
    public void doBefore(JoinPoint point, RateLimit rateLimit) {
        String key = generateRateLimitKey(point, rateLimit);
        // 创建限流器
        RRateLimiter rateLimiter = redissonClient.getRateLimiter(key);

        // 初始化限流器配置（如果不存在或已过期）
        if (!rateLimiter.isExists()) {
            rateLimiter.setRate(RateType.OVERALL, rateLimit.rate(), Duration.ofSeconds(rateLimit.limitInterval()));
            rateLimiter.expire(Duration.ofHours(1));
        }

        // 尝试获取令牌
        if (!rateLimiter.tryAcquire(1)) {
            throw new BusinessException(ErrorCode.TOO_MANY_REQUEST, rateLimit.message());
        }
    }

    /**
     * 生成限流key
     *
     * @param point       切点
     * @param rateLimit 限流注解
     * @return 限流key
     */
    private String generateRateLimitKey(JoinPoint point, RateLimit rateLimit) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append("rate_Limit:");
        // 添加自定义前缀
        if (!rateLimit.key().isEmpty()) {
            keyBuilder.append(rateLimit.key()).append(":");
        }
        // 根据限流类型不同，生成不同的key
        switch (rateLimit.limitType()) {
            case API -> {
                // 接口级别：方法名
                MethodSignature signature = (MethodSignature) point.getSignature();
                Method method = signature.getMethod();
                keyBuilder.append("api:").append(method.getDeclaringClass().getSimpleName())
                        .append(".")
                        .append(method.getName());
            }
            case USER -> {
                // 用户及vi额：用户ID
                try {
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        HttpServletRequest request = attributes.getRequest();
                        Long userId = userService.getLoginUser(request).getId();
                        keyBuilder.append("user:").append(userId);
                    } else {
                        // 无法获取请求上下文，使用IP限流
                        keyBuilder.append("ip:").append(ServletUtil.getClientIP(attributes.getRequest()));
                    }
                } catch (Exception e) {
                    // 获取用户ID失败，使用IP限流
                    ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                    if (attributes != null) {
                        keyBuilder.append("ip:").append(ServletUtil.getClientIP(attributes.getRequest()));
                    } else {
                        keyBuilder.append("ip:unknown");
                    }
                }
            }
            case IP -> {
                // 获取IP地址
                keyBuilder.append("ip:").append(getClientIP());
            }
            default -> throw  new BusinessException(ErrorCode.SYSTEM_ERROR, "限流类型错误");
        }
        return keyBuilder.toString();
    }

}
