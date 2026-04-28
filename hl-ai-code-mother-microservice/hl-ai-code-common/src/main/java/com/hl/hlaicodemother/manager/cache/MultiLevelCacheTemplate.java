package com.hl.hlaicodemother.manager.cache;

import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.github.benmanes.caffeine.cache.Cache;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

/**
 * 多级缓存模板（L1 Caffeine + 进程内单飞 + L2 Redis + Redisson 分布式锁 + RTopic 跨节点 L1 失效）。
 * <p>
 * <b>读路径</b>：
 * <pre>
 * 1) 查 L1（Caffeine）          → 命中(含 NULL 哨兵) 直接返回
 * 2) <b>同 JVM 单飞合并</b>      → 同一 key 只让一个线程往下走，其余 await CompletableFuture
 * 3) 查 L2（Redis）              → 命中则回填 L1 后返回
 * 4) 抢 Redisson RLock           → 抢到才查 DB（双检 L2），抢不到则退避读 L2，仍空则兜底直查
 * 5) DB 加载 → 同时回写 L2 + L1
 * </pre>
 * <p>
 * <b>写路径</b>：{@link #evict(String)} / {@link #evictByPattern(String)} / {@link #delayedEvict(String, Duration)}：
 * 删 L1 + 删 L2 + 通过 {@link RTopic} 通知<b>其它节点</b>同步删它们的 L1。
 *
 * @author hl
 */
@Slf4j
@Component
public class MultiLevelCacheTemplate {

    /** 空值哨兵（写入 L2 时用），与 L1 中的 {@link #L1_NULL_HOLDER} 对应。 */
    private static final String L2_NULL_SENTINEL = "__CACHE_NULL__";

    /** L1 中的 null 占位对象：Caffeine 不允许存 null，得用一个特殊单例。 */
    private static final Object L1_NULL_HOLDER = new Object();

    /** 负缓存 TTL 边界。 */
    private static final Duration MAX_NULL_TTL = Duration.ofMinutes(2);
    private static final Duration MIN_NULL_TTL = Duration.ofSeconds(30);

    /** 分布式锁前缀。 */
    private static final String LOCK_KEY_PREFIX = "lock:cache:";

    /** Redisson 锁默认参数。 */
    private static final long LOCK_WAIT_MILLIS = 200;
    private static final long LOCK_LEASE_MILLIS = 10_000;

    /** 单飞等待超时：避免 leader 卡死时跟随线程被永远阻塞。 */
    private static final long SINGLE_FLIGHT_WAIT_MILLIS = 5_000;

    /** L1 失效广播频道。 */
    private static final String L1_INVALIDATE_TOPIC = "cache:l1:invalidate";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedissonClient redissonClient;

    @Resource
    private Cache<String, Object> l1Cache;

    /**
     * JVM 内"单飞"映射：同一 key 同时间只允许一个线程触发"L2/锁/DB"路径，
     * 其余线程拿到同一个 {@link CompletableFuture} 静候结果。
     */
    private final ConcurrentHashMap<String, CompletableFuture<Object>> inFlight = new ConcurrentHashMap<>();

    /** 当前实例 ID：用来识别 RTopic 消息是否是自己发的，避免回环。 */
    private final String instanceId = IdUtil.fastSimpleUUID();

    private RTopic invalidateTopic;

    @PostConstruct
    public void init() {
        invalidateTopic = redissonClient.getTopic(L1_INVALIDATE_TOPIC);
        // 订阅其它节点广播：同步失效本机 L1
        invalidateTopic.addListener(InvalidateMessage.class, (channel, msg) -> {
            if (msg == null || instanceId.equals(msg.getSender())) {
                return; // 自己发的不处理
            }
            if (InvalidateType.KEY.name().equals(msg.getType())) {
                l1Cache.invalidate(msg.getValue());
            } else if (InvalidateType.PATTERN.name().equals(msg.getType())) {
                invalidateLocalByPattern(msg.getValue());
            } else if (InvalidateType.ALL.name().equals(msg.getType())) {
                l1Cache.invalidateAll();
            }
        });
        log.info("MultiLevelCacheTemplate initialized, instanceId={}", instanceId);
    }

    @PreDestroy
    public void shutdown() {
        // 注意：RTopic 的 listener 由 Redisson 客户端关闭时统一释放
        inFlight.clear();
    }

    // ============================================================
    // 读路径
    // ============================================================

    /**
     * 多级缓存读取（L1 → 单飞 → L2 → 分布式锁 → DB）。
     *
     * @param key    完整缓存 key
     * @param ttl    L2 正常值的 TTL（L1 由 Caffeine 全局策略管控）
     * @param type   反序列化目标类型，可用 {@code new cn.hutool.core.lang.TypeReference<...>(){}.getType()}
     * @param loader 缓存全部未命中时的加载器（通常是 DB 查询）
     */
    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, Duration ttl, Type type, Supplier<T> loader) {
        // 1) L1
        Object l1 = l1Cache.getIfPresent(key);
        if (l1 != null) {
            return l1 == L1_NULL_HOLDER ? null : (T) l1;
        }

        // 2) 单飞合并：同 JVM 内的并发请求只放一个线程往下走
        CompletableFuture<Object> myFuture = new CompletableFuture<>();
        CompletableFuture<Object> existing = inFlight.putIfAbsent(key, myFuture);
        if (existing != null) {
            try {
                Object value = existing.get(SINGLE_FLIGHT_WAIT_MILLIS, TimeUnit.MILLISECONDS);
                return value == L1_NULL_HOLDER ? null : (T) value;
            } catch (TimeoutException e) {
                // leader 还在跑，本线程不再等了，自行走一遍 L2/DB（极端兜底）
                log.warn("Single-flight wait timeout, fallback to local load, key={}", key);
                return loadFromL2OrDb(key, ttl, type, loader);
            } catch (Exception e) {
                // leader 异常 → 本次请求一并失败
                throw new RuntimeException("Single-flight leader failed, key=" + key, e);
            }
        }

        // 我是 leader：负责回源
        try {
            T value = loadFromL2OrDb(key, ttl, type, loader);
            myFuture.complete(value == null ? L1_NULL_HOLDER : value);
            return value;
        } catch (Throwable t) {
            myFuture.completeExceptionally(t);
            throw t;
        } finally {
            inFlight.remove(key, myFuture);
        }
    }

    public <T> T getOrLoad(String key, Duration ttl, Class<T> clazz, Supplier<T> loader) {
        return getOrLoad(key, ttl, (Type) clazz, loader);
    }

    /**
     * L1 已确认未命中的情况下：依次尝试 L2 → 分布式锁 → DB。
     */
    @SuppressWarnings("unchecked")
    private <T> T loadFromL2OrDb(String key, Duration ttl, Type type, Supplier<T> loader) {
        // L2 命中
        T fromL2 = readL2(key, type);
        if (fromL2 != null || isL2Negative(key)) {
            putL1(key, fromL2);
            return fromL2;
        }

        // L2 也 miss → 用 Redisson 锁防击穿
        RLock lock = redissonClient.getLock(LOCK_KEY_PREFIX + key);
        boolean locked = false;
        try {
            locked = lock.tryLock(LOCK_WAIT_MILLIS, LOCK_LEASE_MILLIS, TimeUnit.MILLISECONDS);
            if (locked) {
                // 抢到锁后双检 L2（可能已经被别的进程刚刚回写）
                T again = readL2(key, type);
                if (again != null || isL2Negative(key)) {
                    putL1(key, again);
                    return again;
                }
                // 真正回源
                T value = loader.get();
                writeL2(key, ttl, value);
                putL1(key, value);
                return value;
            }

            // 没拿到锁：退避后再读 L2，前一个持锁者大概率刚回写好
            for (int i = 0; i < 3; i++) {
                sleepQuietly(50L * (1L << i)); // 50ms / 100ms / 200ms
                T retried = readL2(key, type);
                if (retried != null || isL2Negative(key)) {
                    putL1(key, retried);
                    return retried;
                }
            }

            // 仍空 → 兜底直查（不回写，避免与持锁者写竞争）
            log.warn("MultiLevelCache breakdown fallback, key={}", key);
            return loader.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return loader.get();
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                try {
                    lock.unlock();
                } catch (Exception e) {
                    log.warn("Unlock failed, key={}", key, e);
                }
            }
        }
    }

    // ============================================================
    // 写路径：失效
    // ============================================================

    /** 删除单 key：L1 + L2 + 广播。 */
    public void evict(String key) {
        l1Cache.invalidate(key);
        safeDelete(key);
        publish(new InvalidateMessage(InvalidateType.KEY.name(), key, instanceId));
    }

    /** 按前缀模糊删除：L1 + L2 + 广播。 */
    public void evictByPattern(String pattern) {
        invalidateLocalByPattern(pattern);
        safeDeleteByPattern(pattern);
        publish(new InvalidateMessage(InvalidateType.PATTERN.name(), pattern, instanceId));
    }

    /** 延时双删：先删 → delay 后再删一次（覆盖并发回写脏数据的窗口）。 */
    public void delayedEvict(String key, Duration delay) {
        evict(key);
        CompletableFuture
                .delayedExecutor(delay.toMillis(), TimeUnit.MILLISECONDS)
                .execute(() -> evict(key));
    }

    public void delayedEvictByPattern(String pattern, Duration delay) {
        evictByPattern(pattern);
        CompletableFuture
                .delayedExecutor(delay.toMillis(), TimeUnit.MILLISECONDS)
                .execute(() -> evictByPattern(pattern));
    }

    // ============================================================
    // 内部工具
    // ============================================================

    private <T> T readL2(String key, Type type) {
        String cached = safeGet(key);
        if (cached == null || L2_NULL_SENTINEL.equals(cached)) {
            return null;
        }
        return JSONUtil.toBean(cached, type, false);
    }

    private boolean isL2Negative(String key) {
        return L2_NULL_SENTINEL.equals(safeGet(key));
    }

    private void putL1(String key, Object value) {
        l1Cache.put(key, value == null ? L1_NULL_HOLDER : value);
    }

    private <T> void writeL2(String key, Duration ttl, T value) {
        if (value == null) {
            safeSet(key, L2_NULL_SENTINEL, negativeTtl(ttl));
        } else {
            safeSet(key, JSONUtil.toJsonStr(value), jitter(ttl));
        }
    }

    private String safeGet(String key) {
        try {
            return stringRedisTemplate.opsForValue().get(key);
        } catch (Exception e) {
            log.warn("Redis GET failed, treat as miss, key={}", key, e);
            return null;
        }
    }

    private void safeSet(String key, String value, Duration ttl) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, ttl.toMillis(), TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.warn("Redis SET failed, key={}", key, e);
        }
    }

    private void safeDelete(String key) {
        try {
            stringRedisTemplate.delete(key);
        } catch (Exception e) {
            log.warn("Redis DEL failed, key={}", key, e);
        }
    }

    private void safeDeleteByPattern(String pattern) {
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
            log.warn("Redis SCAN+DEL failed, pattern={}", pattern, e);
        }
    }

    /** Caffeine 不支持 pattern 失效：自己按前缀过滤 keyset 删除。 */
    private void invalidateLocalByPattern(String pattern) {
        if (pattern == null || pattern.isEmpty()) {
            return;
        }
        if (pattern.endsWith("*")) {
            String prefix = pattern.substring(0, pattern.length() - 1);
            l1Cache.asMap().keySet().removeIf(k -> k.startsWith(prefix));
        } else {
            l1Cache.invalidate(pattern);
        }
    }

    private void publish(InvalidateMessage msg) {
        try {
            invalidateTopic.publish(msg);
        } catch (Exception e) {
            // 广播失败不影响本地失效；其他节点会等到 L1 自然过期
            log.warn("Publish L1 invalidate failed, msg={}", msg, e);
        }
    }

    private void sleepQuietly(long ms) {
        try {
            TimeUnit.MILLISECONDS.sleep(ms);
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

    // ============================================================
    // 内部消息类型
    // ============================================================

    private enum InvalidateType {
        KEY, PATTERN, ALL
    }

    /**
     * RTopic 广播消息体。需实现 {@link Serializable}：Redisson 默认用 Marshalling 编码 topic 消息。
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvalidateMessage implements Serializable {
        private static final long serialVersionUID = 1L;
        private String type;
        private String value;
        private String sender;
    }
}
