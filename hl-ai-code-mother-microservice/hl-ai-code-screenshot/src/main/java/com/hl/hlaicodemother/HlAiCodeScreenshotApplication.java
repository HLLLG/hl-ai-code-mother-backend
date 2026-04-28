package com.hl.hlaicodemother;

import com.hl.hlaicodemother.config.CosClientConfig;
import com.hl.hlaicodemother.manager.CosManager;
import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication(scanBasePackages = {
        "com.hl.hlaicodemother.bizmq",
        "com.hl.hlaicodemother.service"
})
@EnableDubbo(scanBasePackages = {
        "com.hl.hlaicodemother.bizmq",
        "com.hl.hlaicodemother.service.impl"
})
@Import({CosClientConfig.class, CosManager.class})
public class HlAiCodeScreenshotApplication {
    public static void main(String[] args) {
        SpringApplication.run(HlAiCodeScreenshotApplication.class, args);
    }
}
