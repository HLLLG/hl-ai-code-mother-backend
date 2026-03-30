package com.hl.hlaicodemother.ai;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * AI 代码生成任务管理器，用于控制流式生成的停止信号。
 */
@Component
public class AiGenerationTaskManager {

    private final ConcurrentHashMap<String, TaskContext> taskContextMap = new ConcurrentHashMap<>();

    public TaskContext registerTask(String taskKey) {
        TaskContext taskContext = new TaskContext();
        TaskContext oldTaskContext = taskContextMap.put(taskKey, taskContext);
        if (oldTaskContext != null) {
            oldTaskContext.cancel();
        }
        return taskContext;
    }

    public boolean cancelTask(String taskKey) {
        TaskContext taskContext = taskContextMap.get(taskKey);
        if (taskContext == null) {
            return false;
        }
        taskContext.cancel();
        return true;
    }

    public void removeTask(String taskKey, TaskContext taskContext) {
        taskContextMap.remove(taskKey, taskContext);
    }

    /**
     * 供生成流下游在任务移除前读取取消状态（与 {@link #removeTask} 时序配合使用）
     */
    @Nullable
    public TaskContext getTaskContext(String taskKey) {
        return taskKey == null ? null : taskContextMap.get(taskKey);
    }

    public static class TaskContext {

        private final Sinks.Empty<Void> cancelSink = Sinks.empty();

        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        public Mono<Void> getCancelSignal() {
            return cancelSink.asMono();
        }

        public boolean isCancelled() {
            return cancelled.get();
        }

        public void cancel() {
            if (cancelled.compareAndSet(false, true)) {
                cancelSink.tryEmitEmpty();
            }
        }
    }
}

