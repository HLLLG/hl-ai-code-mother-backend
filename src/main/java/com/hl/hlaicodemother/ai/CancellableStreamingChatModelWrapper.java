package com.hl.hlaicodemother.ai;

import dev.langchain4j.model.ModelProvider;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.ChatRequestParameters;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 可取消的 StreamingChatModel 包装器。
 * <p>
 * langchain4j 的 {@code AiServiceStreamingResponseHandler} 在每一轮工具调用结束后
 * 通过 {@code context.streamingChatModel.chat()} 发起新一轮 LLM 请求。
 * 本包装器在 {@link #chat} 入口处检查取消标记，一旦已取消则直接返回，
 * 不再向底层模型发送请求，从而彻底中断"工具调用 → 新 LLM 请求"的循环。
 */
@Slf4j
public class CancellableStreamingChatModelWrapper implements StreamingChatModel {

    private final StreamingChatModel delegate;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);

    public CancellableStreamingChatModelWrapper(StreamingChatModel delegate) {
        this.delegate = delegate;
    }

    public void setCancelled(boolean value) {
        cancelled.set(value);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public void chat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
        if (cancelled.get()) {
            log.info("StreamingChatModel.chat() 被取消，不再发起新 LLM 请求");
            return;
        }
        delegate.chat(chatRequest, handler);
    }

    @Override
    public void doChat(ChatRequest chatRequest, StreamingChatResponseHandler handler) {
        delegate.doChat(chatRequest, handler);
    }

    @Override
    public ChatRequestParameters defaultRequestParameters() {
        return delegate.defaultRequestParameters();
    }

    @Override
    public List<ChatModelListener> listeners() {
        return delegate.listeners();
    }

    @Override
    public ModelProvider provider() {
        return delegate.provider();
    }
}
