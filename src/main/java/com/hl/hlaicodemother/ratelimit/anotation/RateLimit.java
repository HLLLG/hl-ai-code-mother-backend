package com.hl.hlaicodemother.ratelimit.anotation;

import com.hl.hlaicodemother.ratelimit.enums.RateLimitType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * 限流key
     */
    String key() default "";

    /**
     * 限流类型
     */
    RateLimitType limitType() default RateLimitType.USER;

    /**
     * 限流次数
     */
    int rate() default 5;

    /**
     * 限流时间间隔
     */
    int limitInterval() default 1;

    /**
     * 提示信息
     */
    String message() default "请求过于频繁，请稍后再试";
}
