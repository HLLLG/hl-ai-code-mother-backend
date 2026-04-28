package com.hl.hlaicodeuser;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@MapperScan("com.hl.hlaicodeuser.mapper")
@ComponentScan("com.hl")
@EnableDubbo
public class HlAiCodeUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(HlAiCodeUserApplication.class, args);
    }
}
