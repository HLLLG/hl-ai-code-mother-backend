package com.hl.hlaicodemother.ratelimit.enums;

public enum RateLimitType {

    /**
     * 接口级限流
     */
    API,

    /**
     * 用户级限流
     */
    USER,

    /**
     * IP 级限流
     */
    IP
}
