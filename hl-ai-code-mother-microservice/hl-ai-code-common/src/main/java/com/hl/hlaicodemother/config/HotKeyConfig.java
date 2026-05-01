package com.hl.hlaicodemother.config;

import com.jd.platform.hotkey.client.ClientStarter;
import com.jd.platform.hotkey.client.core.worker.WorkerInfoHolder;
import com.jd.platform.hotkey.client.etcd.EtcdConfigFactory;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Configuration
@ConfigurationProperties(prefix = "hotkey")
@Data
@Slf4j
public class HotKeyConfig {

    /**
     * 是否启用 hotkey 客户端。
     */
    private boolean enabled = true;

    /**
     * 服务侧显式开关。配置后优先于 enabled，避免依赖包或历史配置误覆盖通用 enabled 字段。
     */
    private Boolean clientEnabled;

    /**
     * Etcd 服务器完整地址
     */
    private String etcdServer = "http://127.0.0.1:2379";

    /**
     * 应用名称
     */
    private String appName = "hl-ai-code-mother";

    /**
     * 本地缓存最大数量
     */
    private int caffeineSize = 10000;

    /**
     * 批量推送 key 的间隔时间
     */
    private long pushPeriod = 1000L;

    /**
     * 初始化 hotkey
     */
    @PostConstruct
    public void initHotkey() {
        boolean shouldEnable = clientEnabled != null ? clientEnabled : enabled;
        if (!shouldEnable) {
            log.info("JD-HotKey client is disabled. appName={}, enabled={}, clientEnabled={}",
                    appName, enabled, clientEnabled);
            return;
        }
        log.info("Starting JD-HotKey client. appName={}, enabled={}, clientEnabled={}, etcdServer={}, caffeineSize={}, pushPeriod={}",
                appName, enabled, clientEnabled, etcdServer, caffeineSize, pushPeriod);
        ClientStarter.Builder builder = new ClientStarter.Builder();
        ClientStarter starter = builder.setAppName(appName)
                .setCaffeineSize(caffeineSize)
                .setPushPeriod(pushPeriod)
                .setEtcdServer(etcdServer)
                .build();
        starter.startPipeline();
        log.info("JD-HotKey client started. appName={}", appName);
        logRuntimeStatus();
        CompletableFuture.runAsync(this::logRuntimeStatus, CompletableFuture.delayedExecutor(3, TimeUnit.SECONDS));
    }

    private void logRuntimeStatus() {
        log.info("JD-HotKey runtime status. appName={}, configCenterReady={}, workers={}, nonConnectedWorkers={}",
                appName, EtcdConfigFactory.configCenter() != null, getWorkersText(), WorkerInfoHolder.getNonConnectedWorkers());
    }

    private String getWorkersText() {
        try {
            return String.valueOf(WorkerInfoHolder.class.getMethod("getWorkers").invoke(null));
        } catch (ReflectiveOperationException e) {
            return "unavailable: " + e.getMessage();
        }
    }
}
