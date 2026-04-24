package com.hl.hlaicodemother.monitor;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MonitorContextHolder {

    private static final ThreadLocal<MonitorContext> CONTEXT_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前线程的监控上下文
     *
     * @param context
     */
    public static void set(MonitorContext context) {
        CONTEXT_HOLDER.set(context);
    }

    /**
     * 获取当前线程的监控上下文
     *
     * @return
     */
    public static MonitorContext get() {
        return CONTEXT_HOLDER.get();
    }

    /**
     * 移除当前线程的监控上下文
     */
    public static void remove() {
        CONTEXT_HOLDER.remove();
    }
}
