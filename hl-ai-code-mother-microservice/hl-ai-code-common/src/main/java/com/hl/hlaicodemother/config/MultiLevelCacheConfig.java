package com.hl.hlaicodemother.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * 二级缓存（L1 Caffeine）配置。
 * <p>
 * 默认策略：
 * <ul>
 *     <li>容量上限 10000，超出按 W-TinyLFU 淘汰（Caffeine 默认）</li>
 *     <li>写入后 1 分钟过期：刻意比 L2(Redis) 短，防止陈旧数据滞留太久</li>
 *     <li>记录命中率/加载时长统计，便于通过 metrics 暴露</li>
 * </ul>
 *
 * @author hl
 */
@Configuration
public class MultiLevelCacheConfig {

    @Bean
    public Cache<String, Object> l1Cache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofMinutes(1))
                .recordStats()
                .build();
    }
}
