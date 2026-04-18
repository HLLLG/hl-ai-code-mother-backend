package com.hl.hlaicodemother.manager.cache;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.lang.reflect.Type;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 旁路缓存模板（Cache-Aside Pattern）。
 * <p>
 * 一站式解决缓存层四类典型问题：
 * <ol>
 *     <li><b>缓存穿透</b>：DB 查不到的 key，写入"空值哨兵 + 较短 TTL"，阻止持续穿透。</li>
 *     <li><b>缓存击穿</b>：热点 key 失效瞬间的并发请求，用 Redis 分布式锁让"只有一个查 DB 回源"。</li>
 *     <li><b>缓存雪崩</b>：TTL 抖动 + 异常降级，避免大批 key 同时失效。</li>
 *     <li><b>缓存一致性</b>：提供 {@code evict} / {@code evictByPattern} / {@code delayedEvict}
 *         三种粒度的失效手段，配合 "先更新 DB 再删缓存 + 延时双删" 写策略。</li>
 * </ol>
 *
 * @author hl
 */
@Slf4j
@Component
public class CacheAsideTemplate {

    /** 空值哨兵：刻意选择非业务 JSON 字符串以便区分。 */
    private static final String NULL_SENTINEL = "__CACHE_NULL__";

    /** 负缓存 TTL 上下限。 */
    private static final Duration MAX_NULL_TTL = Duration.ofMinutes(2);
    private static final Duration MIN_NULL_TTL = Duration.ofSeconds(30);

    /** 防击穿锁默认参数。 */
    private static final String LOCK_KEY_PREFIX = "lock:cache:";
    private static final Duration DEFAULT_LOCK_TTL = Duration.ofSeconds(10);
    private static final int LOCK_RETRY_TIMES = 3;
    private static final long LOCK_RETRY_BASE_MS = 50;

    /**
     * 释放锁的 Lua 脚本：值匹配才删除，避免误删别人持有的锁。
     */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
            Long.class
    );

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 用于"延时双删"等异步缓存操作的小型调度器。
     * 单线程足够：删除 Redis key 是 O(1) 网络往返，且任务不应阻塞。
     */
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "cache-aside-scheduler");
        t.setDaemon(true);
        return t;
    });

    @PreDestroy
    public void shutdown() {
        scheduler.shutdownNow();
    }

    // ============================================================
    // 读路径：旁路缓存
    // ============================================================

    /**
     * 旁路缓存读取（基础版）。适合读多写多但<b>不是热 key</b>的场景。
     * 热 key 请使用 {@link #getOrLoadWithLock(String, Duration, Type, Supplier)} 防击穿。
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
        // 3) 回写缓存：null → 哨兵 + 较短 TTL；非 null → 正常 JSON + 抖动 TTL
        writeBack(key, ttl, value);
        return value;
    }

    public <T> T getOrLoad(String key, Duration ttl, Class<T> clazz, Supplier<T> loader) {
        return getOrLoad(key, ttl, (Type) clazz, loader);
    }

    /**
     * 旁路缓存读取（带分布式锁，<b>防缓存击穿</b>）。
     * <p>
     * 流程：
     * <pre>
     * 1) 读缓存 → 命中（含哨兵）则直接返回
     * 2) 未命中 → 抢分布式锁（SET NX PX）
     *    a) 抢到锁 → 再读一次缓存（双检），仍未命中才查 DB → 回写 → 释放锁
     *    b) 没抢到 → 短暂退避后重试读缓存（前一个线程通常已经回写好了）
     * 3) 多次重试仍未命中 → 兜底直查 DB（保证可用性，宁可多打几次也别返回错的）
     * </pre>
     */
    public <T> T getOrLoadWithLock(String key, Duration ttl, Type type, Supplier<T> loader) {
        // 1) 先读
        T fast = readIfPresent(key, type);
        if (fast != null || isNegativelyCached(key)) {
            return fast;
        }

        // 2) 抢锁回源
        String lockKey = LOCK_KEY_PREFIX + key;
        String token = IdUtil.fastSimpleUUID();
        if (tryLock(lockKey, token, DEFAULT_LOCK_TTL)) {
            try {
                // 双检：可能在抢锁期间别人已经回写
                T again = readIfPresent(key, type);
                if (again != null || isNegativelyCached(key)) {
                    return again;
                }
                T value = loader.get();
                writeBack(key, ttl, value);
                return value;
            } finally {
                unlock(lockKey, token);
            }
        }

        // 3) 没抢到锁 → 短暂退避后再读缓存
        for (int i = 0; i < LOCK_RETRY_TIMES; i++) {
            sleepQuietly(LOCK_RETRY_BASE_MS * (1L << i)); // 50ms / 100ms / 200ms
            T retried = readIfPresent(key, type);
            if (retried != null || isNegativelyCached(key)) {
                return retried;
            }
        }

        // 4) 兜底：直查 DB，但不回写（避免和锁持有者写竞争）
        log.warn("Cache breakdown fallback (no lock, no cache), key={}", key);
        return loader.get();
    }

    // ============================================================
    // 写路径：失效策略
    // ============================================================

    /**
     * 删除单个 key。推荐在数据变更<b>事务提交后</b>调用。
     */
    public void evict(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis EVICT failed, key={}", key, e);
        }
    }

    /**
     * 按 pattern 批量失效。<b>用 SCAN 而非 KEYS，避免阻塞 Redis。</b>
     * <p>
     * 适用场景：列表/分页类缓存——一次写入会让多个 page key 失效，无法精确删，
     * 此时按业务前缀（如 {@code "good_app_page:*"}）整体清掉是最稳的做法。
     */
    public void evictByPattern(String pattern) {
        try (Cursor<String> cursor = stringRedisTemplate.scan(
                ScanOptions.scanOptions().match(pattern).count(200).build())) {
            Set<String> batch = new HashSet<>(256);
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= 200) {
                    stringRedisTemplate.delete(batch);
                    batch.clear();
                }
            }
            if (!batch.isEmpty()) {
                stringRedisTemplate.delete(batch);
            }
        } catch (Exception e) {
            log.warn("Redis EVICT BY PATTERN failed, pattern={}", pattern, e);
        }
    }

    /**
     * 延时双删：先立即删一次，再在 {@code delay} 后异步删一次。
     * <p>
     * 解决"读请求拿到旧值 → 回写缓存 → 我们已经删过 → 缓存又脏了"的并发缝隙。
     * 延时一般略大于一次"读 DB + 回写"的耗时（经验值 300ms~1s）。
     */
    public void delayedEvict(String key, Duration delay) {
        evict(key);
        scheduler.schedule(() -> evict(key), delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    /**
     * 延时双删（pattern 版本）。
     */
    public void delayedEvictByPattern(String pattern, Duration delay) {
        evictByPattern(pattern);
        scheduler.schedule(() -> evictByPattern(pattern), delay.toMillis(), TimeUnit.MILLISECONDS);
    }

    // ============================================================
    // 内部工具
    // ============================================================

    private <T> T readIfPresent(String key, Type type) {
        String cached = safeGet(key);
        if (cached == null || NULL_SENTINEL.equals(cached)) {
            return null;
        }
        return JSONUtil.toBean(cached, type, false);
    }

    private boolean isNegativelyCached(String key) {
        return NULL_SENTINEL.equals(safeGet(key));
    }

    private <T> void writeBack(String key, Duration ttl, T value) {
        if (value == null) {
            safeSet(key, NULL_SENTINEL, negativeTtl(ttl));
        } else {
            safeSet(key, JSONUtil.toJsonStr(value), jitter(ttl));
        }
    }

    private String safeGet(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis GET failed, fallback to loader, key={}", key, e);
            return null;
        }
    }

    private void safeSet(String key, String value, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("Redis SET failed, skip caching, key={}", key, e);
        }
    }

    private boolean tryLock(String lockKey, String token, Duration ttl) {
        try {
            Boolean ok = stringRedisTemplate.opsForValue()
                    .setIfAbsent(lockKey, token, ttl.toMillis(), TimeUnit.MILLISECONDS);
            return Boolean.TRUE.equals(ok);
        } catch (Exception e) {
            // 锁服务不可用 → 视为未获取到锁，由调用方走兜底逻辑
            log.warn("Redis tryLock failed, treat as not acquired, lockKey={}", lockKey, e);
            return false;
        }
    }

    private void unlock(String lockKey, String token) {
        try {
            stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), token);
        } catch (Exception e) {
            log.warn("Redis unlock failed, lockKey={}", lockKey, e);
        }
    }

    private void sleepQuietly(long ms) {
        try {
            // 加一点随机抖动，进一步分散重试压力
            TimeUnit.MILLISECONDS.sleep(ms + ThreadLocalRandom.current().nextLong(20));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private Duration jitter(Duration ttl) {
        long ms = ttl.toMillis();
        long add = RandomUtil.randomLong(0, Math.max(1, ms / 10));
        return Duration.ofMillis(ms + add);
    }

    private Duration negativeTtl(Duration ttl) {
        long fifth = ttl.toMillis() / 5;
        long ms = Math.min(MAX_NULL_TTL.toMillis(), Math.max(MIN_NULL_TTL.toMillis(), fifth));
        return Duration.ofMillis(ms);
    }
}
