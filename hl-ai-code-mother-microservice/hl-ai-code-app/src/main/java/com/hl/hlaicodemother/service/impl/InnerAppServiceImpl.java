package com.hl.hlaicodemother.service.impl;

import com.hl.hlaicodemother.innerservice.InnerAppService;
import com.hl.hlaicodemother.model.entity.App;
import com.hl.hlaicodemother.service.AppService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

@DubboService
public class InnerAppServiceImpl implements InnerAppService {

    @Resource
    private AppService appService;

    @Override
    public boolean updateById(App app) {
        return appService.updateById(app);
    }

}

