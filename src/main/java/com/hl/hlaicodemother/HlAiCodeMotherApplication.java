package com.hl.hlaicodemother;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.hl.hlaicodemother.mapper")
public class HlAiCodeMotherApplication {

    public static void main(String[] args) {
        SpringApplication.run(HlAiCodeMotherApplication.class, args);
    }

}
