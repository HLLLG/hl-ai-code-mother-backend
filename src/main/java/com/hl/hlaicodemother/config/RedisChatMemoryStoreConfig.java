package com.hl.hlaicodemother.config;

import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * redis 对话记忆存储配置
 */
@Component
@ConfigurationProperties(prefix = "spring.data.redis")
@Data
public class RedisChatMemoryStoreConfig {

    private String host;
    private int port;
    private String password;
    private long ttl;

    @Bean
    public MyRedisChatMemoryStore redisChatMemoryStore(){
        return MyRedisChatMemoryStore.builder()
                .host(host)
                .port(port)
                .password(password)
                .ttl(ttl)
                .build();
    }

    @Bean
    public ChatMemoryProvider chatMemoryProvider(MyRedisChatMemoryStore myRedisChatMemoryStore){
        // 根据id构建独立的会话记忆
        return memoryId -> MessageWindowChatMemory.builder()
                .chatMemoryStore(myRedisChatMemoryStore)
                .id(memoryId)
                .maxMessages(20) // 设置最大消息数为20
                .build();
    }
}
