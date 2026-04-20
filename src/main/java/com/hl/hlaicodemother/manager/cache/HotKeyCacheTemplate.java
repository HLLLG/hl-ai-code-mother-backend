package com.hl.hlaicodemother.manager.cache;

import com.jd.platform.hotkey.client.callback.JdHotKeyStore;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    @SuppressWarnings("unchecked")
    public <T> T getOrLoad(String key, Class<T> clazz, Supplier<T> loader) {
        Object cached = JdHotKeyStore.getValue(key);
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
            JdHotKeyStore.remove(key);
        }
        T value = loader.get();
        JdHotKeyStore.smartSet(key, value == null ? NULL_HOLDER : value);
        return value;
    }

    public void evict(String key) {
        JdHotKeyStore.remove(key);
    }
}
