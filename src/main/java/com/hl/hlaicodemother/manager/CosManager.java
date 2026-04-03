package com.hl.hlaicodemother.manager;

import com.hl.hlaicodemother.config.CosClientConfig;
import com.hl.hlaicodemother.exception.BusinessException;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;

@Component
@Slf4j
public class CosManager {

    @Resource
    private COSClient cosClient;

    @Resource
    private CosClientConfig cosClientConfig;

    /**
     * 上传对象
     *
     * @param key
     * @param file
     * @return
     */
    public PutObjectResult putObject(String key, File file) {
        String bucketName = cosClientConfig.getBucket();
        PutObjectRequest putObjectRequest = new PutObjectRequest(bucketName, key, file);
        try {
            return cosClient.putObject(putObjectRequest);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "上传文件失败" +  e.getMessage());
        } finally {
            // 确认本进程不再使用 cosClient 实例之后，关闭即可
            cosClient.shutdown();
        }
    }

    /**
     * 上传文件
     *
     * @param key
     * @param file
     * @return
     */
    public String uploadFile(String key, File file) {
        PutObjectResult putObjectResult = putObject(key, file);
        if (putObjectResult != null) {
            String url = String.format("%s%s", cosClientConfig.getHost(), key);
            log.info("上传文件到cos成功: {} -> {}", file.getName(), url);
            return url;
        } else {
            log.error("上传文件到cos失败: {}, 返回结果空", file.getName());
            return null;
        }
    }
}
