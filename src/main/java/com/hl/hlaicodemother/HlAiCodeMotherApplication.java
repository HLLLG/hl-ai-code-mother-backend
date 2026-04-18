package com.hl.hlaicodemother;

import dev.langchain4j.community.store.embedding.redis.spring.RedisEmbeddingStoreAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication(exclude = RedisEmbeddingStoreAutoConfiguration.class)
@EnableCaching
@MapperScan("com.hl.hlaicodemother.mapper")
public class HlAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(HlAiCodeMotherApplication.class, args);
    }

}
