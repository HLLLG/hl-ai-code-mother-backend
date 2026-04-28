package com.hl.hlaicodemother.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 客户端配置。
 * <p>
 * 仅暴露 {@link RedissonClient} Bean，用于：
 * <ul>
 *     <li>分布式锁（RLock）—— 防缓存击穿</li>
 *     <li>发布/订阅（RTopic）—— 跨节点 L1 缓存失效广播</li>
 * </ul>
 * 不接管 Spring Data Redis 的 {@code RedisConnectionFactory}，
 * 让既有的 {@link org.springframework.data.redis.core.StringRedisTemplate} 继续走 Lettuce。
 *
 * @author hl
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.data.redis.host:localhost}")
    private String host;

    @Value("${spring.data.redis.port:6379}")
    private int port;

    @Value("${spring.data.redis.password:}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + host + ":" + port)
                .setDatabase(database)
                .setPassword((password == null || password.isEmpty()) ? null : password)
                .setConnectionMinimumIdleSize(4)
                .setConnectionPoolSize(16)
                // 锁 watchdog 默认 30s（这里显式声明便于阅读）
                .setIdleConnectionTimeout(10_000)
                .setConnectTimeout(5_000)
                .setTimeout(3_000)
                .setRetryAttempts(3);
        return Redisson.create(config);
    }
}
