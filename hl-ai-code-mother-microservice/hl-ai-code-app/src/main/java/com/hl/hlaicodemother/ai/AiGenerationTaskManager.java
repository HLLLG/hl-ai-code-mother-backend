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

    /**
     * 注册新的 AI 生成任务并返回任务上下文。
     *
     * @param taskKey 任务的唯一标识键
     * @return 新创建的任务上下文对象，用于控制任务的取消操作
     */
    public TaskContext registerTask(String taskKey) {
        TaskContext taskContext = new TaskContext();
        TaskContext oldTaskContext = taskContextMap.put(taskKey, taskContext);
        if (oldTaskContext != null) {
            oldTaskContext.cancel();
        }
        return taskContext;
    }


    /**
     * 取消指定的 AI 生成任务。
     *
     * @param taskKey 任务的唯一标识键
     * @return 是否成功取消任务
     */
    public boolean cancelTask(String taskKey) {
        TaskContext taskContext = taskContextMap.get(taskKey);
        if (taskContext == null) {
            return false;
        }
        taskContext.cancel();
        return true;
    }

    /**
     * 从任务映射表中移除指定的任务。
     * <p>
     * 仅当给定的任务键和任务上下文对象都与当前存储的值匹配时才会执行移除操作，
     * 确保并发安全。
     *
     * @param taskKey 任务的唯一标识键
     * @param taskContext 要移除的任务上下文对象
     */
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

