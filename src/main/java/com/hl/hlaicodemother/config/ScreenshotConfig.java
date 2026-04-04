package com.hl.hlaicodemother.config;

import com.hl.hlaicodemother.bizmq.ScreenshotMessageProducer;
import com.hl.hlaicodemother.bizmq.model.ScreenshotTaskMessage;
import com.hl.hlaicodemother.constant.AppConstant;
import com.hl.hlaicodemother.model.entity.App;
import com.hl.hlaicodemother.service.AppService;
import com.mybatisflex.core.query.QueryWrapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

@Configuration
@EnableScheduling
@Slf4j
public class ScreenshotConfig {

    @Resource
    private AppService appService;

    @Resource
    private ScreenshotMessageProducer screenshotMessageProducer;

    /**
     * 每天凌晨2点检查数据库中没有封面的应用并生成封面
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void checkAndGenerateScreenshot() {

            log.info("开始检查数据库中没有封面的应用，并生成封面");
            // 获得所有已部署无封面的应用
            QueryWrapper queryWrapper = new QueryWrapper()
                    .isNotNull(App::getDeployKey)
                    .isNull(App::getCover)
                    .isNotNull(App::getCurrentVersion);;
            appService.list(queryWrapper).forEach(app -> {
                try {
                log.info("开始生成封面，appId={}", app.getId());
                // 构造部署地址
                String deployUrl = String.format("%s/%s/v%s/", AppConstant.CODE_DEPLOY_HOST,
                        app.getDeployKey(), app.getCurrentVersion());
                screenshotMessageProducer.sendScreenshotTask(new ScreenshotTaskMessage(app.getId(), deployUrl));
                } catch (Exception e) {
                    log.error("生成封面失败，appId={}", app.getId(), e);
                }
            });
            log.info("生成封面完成");
    }

}
