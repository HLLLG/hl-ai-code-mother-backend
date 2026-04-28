package com.hl.hlaicodemother.config;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.region.Region;
import lombok.Data;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cos.client")
@ConditionalOnProperty(
        prefix = "cos.client",
        name = {"host", "secretId", "secretKey", "region", "bucket"}
)
@Data
public class CosClientConfig {

    /**
     * 密钥id
     */
    private String secretId;

    /**
     * 密钥
     */
    private String secretKey;

    /**
     * 存储桶
     */
    private String bucket;

    /**
     * 区域
     */
    private String region;

    /**
     * 域名
     */
    private String host;

    @Bean
    public COSClient cosClient() {
        // 1 初始化用户身份信息(secretId, secretKey)
        COSCredentials cred = new BasicCOSCredentials(secretId, secretKey);
        // 2 设置 bucket 的区域
        ClientConfig clientConfig = new ClientConfig(new Region(region));
        // 3 生成 cos 客户端
        return new COSClient(cred, clientConfig);
    }
}
