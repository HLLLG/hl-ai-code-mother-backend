package com.hl.hlaicodemother.config;

import dev.langchain4j.community.store.memory.chat.redis.RedisChatMemoryStoreException;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.internal.ValidationUtils;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import redis.clients.jedis.JedisPooled;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.json.DefaultGsonObjectMapper;
import redis.clients.jedis.json.JsonObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * 自定义Redis会话记忆存储
 */
public class MyRedisChatMemoryStore implements ChatMemoryStore {

    private final JedisPooled client;
    private final String keyPrefix;
    private final Long ttl;
    private JsonObjectMapper jsonMapper;

    public MyRedisChatMemoryStore(String host, Integer port, String user, String password) {
        this(host, port, user, password, "", 0L);
    }

    public MyRedisChatMemoryStore(String host, Integer port, String user, String password, String prefix, Long ttl) {
        String finalHost = ValidationUtils.ensureNotBlank(host, "host");
        int finalPort = (Integer)ValidationUtils.ensureNotNull(port, "port");
        if (user != null) {
            String finalUser = ValidationUtils.ensureNotBlank(user, "user");
            String finalPassword = ValidationUtils.ensureNotBlank(password, "password");
            this.client = new JedisPooled(finalHost, finalPort, finalUser, finalPassword);
        } else {
            this.client = new JedisPooled(finalHost, finalPort);
        }

        this.keyPrefix = (String)ValidationUtils.ensureNotNull(prefix, "prefix");
        this.ttl = (Long)ValidationUtils.ensureNotNull(ttl, "ttl");
        this.jsonMapper = new DefaultGsonObjectMapper();
    }

    @Override
    public List<ChatMessage> getMessages(Object memoryId) {
//        String json = this.jsonMapper.toJson(this.client.jsonGet(this.toRedisKey(memoryId)));
//        return (List<ChatMessage>)(json == null ? new ArrayList() : ChatMessageDeserializer.messagesFromJson(json));
        String json = this.client.get(this.toRedisKey(memoryId));
        return (List<ChatMessage>)(json == null ? new ArrayList() : ChatMessageDeserializer.messagesFromJson(json));
    }

    @Override
    public void updateMessages(Object memoryId, List<ChatMessage> messages) {
        String json = ChatMessageSerializer.messagesToJson((List)ValidationUtils.ensureNotEmpty(messages, "messages"));
        String key = this.toRedisKey(memoryId);
        String res;
        if (this.ttl > 0L) {
            try (Pipeline pipeline = this.client.pipelined()) {
//                pipeline.jsonSet(key, json);
//                pipeline.expire(key, this.ttl);
                pipeline.set(key, json);
                pipeline.expire(key, this.ttl);
                List<Object> results = pipeline.syncAndReturnAll();
                res = (String)results.get(0);
            }
        } else {
//            res = this.client.jsonSet(key, json);
            res = this.client.set(key, json);
        }

        if (!"OK".equals(res)) {
            throw new RedisChatMemoryStoreException("Set memory error, msg=" + res);
        }
    }

    @Override
    public void deleteMessages(Object memoryId) {
//        this.client.jsonDel(this.toRedisKey(memoryId));
        this.client.del(this.toRedisKey(memoryId));
    }

    private String toMemoryIdString(Object memoryId) {
        boolean isNullOrEmpty = memoryId == null || memoryId.toString().trim().isEmpty();
        if (isNullOrEmpty) {
            throw new IllegalArgumentException("memoryId cannot be null or empty");
        } else {
            return memoryId.toString();
        }
    }

    private String toRedisKey(Object memoryId) {
        String var10000 = this.keyPrefix;
        return var10000 + this.toMemoryIdString(memoryId);
    }

    public static MyRedisChatMemoryStore.Builder builder() {
        return new MyRedisChatMemoryStore.Builder();
    }

    public static class Builder {
        private String host;
        private Integer port;
        private String user;
        private String password;
        private Long ttl = 0L;
        private String prefix = "";

        public MyRedisChatMemoryStore.Builder host(String host) {
            this.host = host;
            return this;
        }

        public MyRedisChatMemoryStore.Builder port(Integer port) {
            this.port = port;
            return this;
        }

        public MyRedisChatMemoryStore.Builder user(String user) {
            this.user = user;
            return this;
        }

        public MyRedisChatMemoryStore.Builder password(String password) {
            this.password = password;
            return this;
        }

        public MyRedisChatMemoryStore.Builder ttl(Long ttl) {
            this.ttl = ttl;
            return this;
        }

        public MyRedisChatMemoryStore.Builder prefix(String prefix) {
            this.prefix = prefix;
            return this;
        }

        public MyRedisChatMemoryStore build() {
            return new MyRedisChatMemoryStore(this.host, this.port, this.user, this.password, this.prefix, this.ttl);
        }
    }
}
