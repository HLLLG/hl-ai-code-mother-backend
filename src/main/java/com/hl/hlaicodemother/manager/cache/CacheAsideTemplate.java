package com.hl.hlaicodemother.manager.cache;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 旁路缓存模板（Cache-Aside Pattern）。
 * <p>
 * 统一解决三类典型问题：
 * <ol>
 *     <li><b>缓存穿透</b>：数据源查不到的 key，用"空值哨兵 + 较短 TTL"回写缓存，阻止后续穿透到 DB。</li>
 *     <li><b>缓存雪崩</b>：对 TTL 做随机抖动，避免大量 key 同一时刻集中失效。</li>
 *     <li><b>Redis 抖动/宕机</b>：读写 Redis 的异常一律捕获，<b>降级直查数据源</b>，保证业务可用。</li>
 * </ol>
 * <p>
 * 使用示例：
 * <pre>{@code
 * Page<AppVO> page = cacheAsideTemplate.getOrLoad(
 *         key,
 *         Duration.ofMinutes(5),
 *         new TypeReference<Page<AppVO>>() {}.getType(),
 *         () -> queryFromDb(request)
 * );
 * }</pre>
 *
 * @author hl
 */
@Slf4j
@Component
public class CacheAsideTemplate {

    /**
     * 空值哨兵。刻意选择一个"不可能是业务 JSON"的普通字符串，
     * 读到它就代表"已确认数据源无数据"，用以区分"未命中"。
     */
    private static final String NULL_SENTINEL = "__CACHE_NULL__";

    /**
     * 负缓存 TTL 上限：不宜过长，避免业务新增数据长时间读不到。
     */
    private static final Duration MAX_NULL_TTL = Duration.ofMinutes(2);

    /**
     * 负缓存 TTL 下限。
     */
    private static final Duration MIN_NULL_TTL = Duration.ofSeconds(30);

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 泛型版本：用于 Page / List / Map 等带泛型的返回值。
     *
     * @param key    完整 Redis key（建议通过 {@code CacheKeyUtils.generateKeyWithPrefix} 生成）
     * @param ttl    正常值的 TTL
     * @param type   反序列化目标类型，一般用 {@code new cn.hutool.core.lang.TypeReference<Page<AppVO>>(){}.getType()}
     * @param loader 缓存未命中时的加载逻辑（通常是查 DB）
     * @param <T>    返回值类型
     * @return 业务数据；若 loader 也返回 null，则返回 null（此时会写入空值哨兵）
     */
    public <T> T getOrLoad(String key, Duration ttl, Type type, Supplier<T> loader) {
        // 1) 读缓存：异常时 safeGet 返回 null，即按"未命中"处理，降级到 loader
        String cached = safeGet(key);
        if (NULL_SENTINEL.equals(cached)) {
            // 命中负缓存 → 明确告知调用方"库里就没这条数据"，不再打 DB
            return null;
        }
        if (cached != null) {
            // 命中真实数据
            return JSONUtil.toBean(cached, type, false);
        }

        // 2) 未命中 → 走数据源
        T value = loader.get();

        // 3) 回写缓存：null → 哨兵；非 null → 正常 JSON
        if (value == null) {
            safeSet(key, NULL_SENTINEL, negativeTtl(ttl));
        } else {
            safeSet(key, JSONUtil.toJsonStr(value), jitter(ttl));
        }
        return value;
    }

    /**
     * Class 版本：适用于无泛型的 POJO。
     */
    public <T> T getOrLoad(String key, Duration ttl, Class<T> clazz, Supplier<T> loader) {
        return getOrLoad(key, ttl, (Type) clazz, loader);
    }

    /**
     * 主动失效（写后删）。推荐在数据变更的事务<em>提交后</em>调用。
     */
    public void evict(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis EVICT failed, key={}", key, e);
        }
    }

    // -------------------- 内部工具 --------------------

    private String safeGet(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            // Redis 故障时不抛给业务：返回 null 让上层走 loader，避免缓存层拖垮业务
            log.warn("Redis GET failed, fallback to loader, key={}", key, e);
            return null;
        }
    }

    private void safeSet(String key, String value, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            // 写失败不影响本次请求，下次还会再尝试
            log.warn("Redis SET failed, skip caching, key={}", key, e);
        }
    }

    /**
     * TTL 抖动：在 [ttl, ttl + 10%] 区间随机，缓解缓存雪崩。
     */
    private Duration jitter(Duration ttl) {
        long ms = ttl.toMillis();
        long add = RandomUtil.randomLong(0, Math.max(1, ms / 10));
        return Duration.ofMillis(ms + add);
    }

    /**
     * 负缓存 TTL：取主 TTL 的 1/5，并钳制在 [MIN_NULL_TTL, MAX_NULL_TTL] 之间。
     */
    private Duration negativeTtl(Duration ttl) {
        long fifth = ttl.toMillis() / 5;
        long ms = Math.min(MAX_NULL_TTL.toMillis(), Math.max(MIN_NULL_TTL.toMillis(), fifth));
        return Duration.ofMillis(ms);
    }
}
