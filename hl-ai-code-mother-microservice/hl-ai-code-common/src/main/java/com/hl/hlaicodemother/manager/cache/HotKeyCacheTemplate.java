package com.hl.hlaicodemother.manager.cache;

import com.jd.platform.hotkey.client.callback.JdHotKeyStore;
import com.jd.platform.hotkey.client.etcd.EtcdConfigFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * JD-HotKey 缓存模板。
 * 负责封装 getValue / smartSet / remove，并统一处理空值哨兵。
 */
@Slf4j
@Component
public class HotKeyCacheTemplate {

    /**
     * 本地缓存不支持 null，使用哨兵对象表示"查询结果为空"。
     */
    private static final Object NULL_HOLDER = new Object();

    private final AtomicBoolean notReadyLogged = new AtomicBoolean(false);

    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, Class<T> clazz, Supplier<T> loader) {
        if (!isHotKeyReady("getOrLoad", key)) {
            return loader.get();
        }
        Object cached;
        try {
            cached = JdHotKeyStore.getValue(key);
        } catch (RuntimeException e) {
            log.warn("JD-HotKey get failed, fallback to loader. key={}", key, e);
            return loader.get();
        }
        if (cached == NULL_HOLDER) {
            return null;
        }
        if (cached != null) {
            if (clazz.isInstance(cached)) {
                return (T) cached;
            }
            // 理论上不会出现类型错配，做一层兜底避免脏值长期驻留。
            log.warn("HotKey cache type mismatch, evict stale value. key={}, expect={}, actual={}",
                    key, clazz.getName(), cached.getClass().getName());
            removeQuietly(key);
        }
        T value = loader.get();
        try {
            JdHotKeyStore.smartSet(key, value == null ? NULL_HOLDER : value);
        } catch (RuntimeException e) {
            log.warn("JD-HotKey set failed, skip cache write. key={}", key, e);
        }
        return value;
    }

    public void evict(String key) {
        if (!isHotKeyReady("evict", key)) {
            return;
        }
        removeQuietly(key);
    }

    private boolean isHotKeyReady(String operation, String key) {
        if (EtcdConfigFactory.configCenter() != null) {
            return true;
        }
        if (notReadyLogged.compareAndSet(false, true)) {
            log.warn("JD-HotKey client is not initialized, skip hotkey cache operation. operation={}, key={}",
                    operation, key);
        }
        return false;
    }

    private void removeQuietly(String key) {
        try {
            JdHotKeyStore.remove(key);
        } catch (RuntimeException e) {
            log.warn("JD-HotKey remove failed, skip cache eviction. key={}", key, e);
        }
    }
}
