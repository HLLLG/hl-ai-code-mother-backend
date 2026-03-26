package com.hl.hlaicodemother.core;

import cn.hutool.core.util.StrUtil;
import com.hl.hlaicodemother.ai.AiCodeGeneratorService;
import com.hl.hlaicodemother.ai.model.HtmlCodeResult;
import com.hl.hlaicodemother.ai.model.MultiFileCodeResult;
import com.hl.hlaicodemother.core.parser.CodeParserExecutor;
import com.hl.hlaicodemother.core.saver.CodeFileSaverExecutor;
import com.hl.hlaicodemother.exception.BusinessException;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.exception.ThrowUtils;
import com.hl.hlaicodemother.model.entity.App;
import com.hl.hlaicodemother.model.entity.AppVersion;
import com.hl.hlaicodemother.model.enums.CodeGenTypeEnum;
import com.hl.hlaicodemother.service.AppService;
import com.hl.hlaicodemother.service.AppVersionService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Flux;

import java.io.File;

/**
 * AI 代码生成器门面类
 */
@Service
@Slf4j
public class AiCodeGeneratorFacade {

    @Resource
    private AiCodeGeneratorService aiCodeGeneratorService;

    @Resource
    private AppVersionService appVersionService;

    @Resource
    private AppService appService;
    @Autowired
    private TransactionTemplate transactionTemplate;

    /**
     * 统一入口，根据类型生成并保存代码
     *
     * @param userMessage
     * @param codeGenType
     * @return
     */
    public File generateAndSaveCode(String userMessage, CodeGenTypeEnum codeGenType, App app) {
        // 校验参数
        if (StrUtil.isBlank(userMessage)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户输入不能为空");
        }
        if (codeGenType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
        }
        // 根据类型生成代码
        return switch (codeGenType) {
            case HTML -> {
                HtmlCodeResult htmlCodeResult = aiCodeGeneratorService.generateHtmlCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(htmlCodeResult, CodeGenTypeEnum.HTML, app.getId(),
                        app.getCurrentVersion());
            }
            case MULTI_FILE -> {
                MultiFileCodeResult multiFileCodeResult = aiCodeGeneratorService.generateMultiFileCode(userMessage);
                yield CodeFileSaverExecutor.executeSaver(multiFileCodeResult, CodeGenTypeEnum.MULTI_FILE, app.getId()
                        , app.getCurrentVersion());
            }
            default -> {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的代码生成类型: " + codeGenType.getValue());
            }
        };
    }

    /**
     * 统一入口，根据类型生成并保存代码（流式版本）
     *
     * @param userMessage
     * @param codeGenType
     * @return
     */
    public Flux<String> generateAndSaveCodeStream(String userMessage, CodeGenTypeEnum codeGenType, App app) {
        // 校验参数
        if (StrUtil.isBlank(userMessage)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户输入不能为空");
        }
        if (codeGenType == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "代码生成类型不能为空");
        }
        // 根据类型生成代码
        return switch (codeGenType) {
            case HTML -> {
                // 调用 AI 生成 HTML 代码结果（流式）
                Flux<String> htmlCodeStream = aiCodeGeneratorService.generateHtmlCodeStream(userMessage);
                yield processCodeStream(htmlCodeStream, CodeGenTypeEnum.HTML, app, userMessage);
            }
            case MULTI_FILE -> {
                // 调用 AI 生成多文件代码结果（流式）
                Flux<String> multiFileCodeStream = aiCodeGeneratorService.generateMultiFileStream(userMessage);
                yield processCodeStream(multiFileCodeStream, CodeGenTypeEnum.MULTI_FILE, app, userMessage);
            }
            default -> {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的代码生成类型: " + codeGenType.getValue());
            }
        };
    }

    /**
     * 处理流式代码生成结果，实时接收代码片段并在完成后保存完整代码
     *
     * @param codeStream
     * @param codeGenType
     * @return
     */
    private Flux<String> processCodeStream(Flux<String> codeStream, CodeGenTypeEnum codeGenType, App app,
                                           String userMessage) {
        // 当流式返沪生成代码完成后，再保存代码
        StringBuilder codeBuilder = new StringBuilder();
        // 实时收集代码片段
        return codeStream.doOnNext(codeBuilder::append).doOnComplete(() -> {
            // 流式生成完成后，保存代码到本地
            try {
                String completeResult = codeBuilder.toString();
                Object parserResult = CodeParserExecutor.executeParser(completeResult, codeGenType);
                Integer currentVersion = app.getCurrentVersion();
                int version = currentVersion;
                Integer lastedVersion = app.getLastedVersion();
                App updateApp = new App();
                AppVersion newAppVersion = new AppVersion();
                // 开启事务

                // 若最新版本号不为1，则表示是增量生成，需要在原有版本基础上进行版本迭代
                if (lastedVersion > 1) {
                    transactionTemplate.execute(status -> {
                        // 获取当前版本信息，进行版本迭代
                        Long currentVersionId = app.getCurrentVersionId();
                        AppVersion oldAppVersion = appVersionService.getById(currentVersionId);
                        ThrowUtils.throwIf(oldAppVersion == null, ErrorCode.NOT_FOUND_ERROR, "当前版本信息不存在");
                        BeanUtils.copyProperties(oldAppVersion, newAppVersion);
                        newAppVersion.setId(null);
                        newAppVersion.setUserPrompt(userMessage);
                        newAppVersion.setVersion(currentVersion + 1);

                        boolean appVersionResult = appVersionService.save(newAppVersion);
                        ThrowUtils.throwIf(!appVersionResult, ErrorCode.OPERATION_ERROR, "保存版本信息失败");
                        // 更新应用的当前版本信息
                        updateApp.setId(app.getId());
                        updateApp.setLastedVersion(lastedVersion + 1);
                        updateApp.setCurrentVersion(currentVersion + 1);
                        updateApp.setCurrentVersionId(newAppVersion.getId());
                        return null;
                    });
                    version += 1;
                } else {
                    updateApp.setId(app.getId());
                    updateApp.setLastedVersion(lastedVersion + 1);
                }

                boolean appResult = appService.updateById(updateApp);
                ThrowUtils.throwIf(!appResult, ErrorCode.OPERATION_ERROR, "更新应用版本信息失败");


                File saveDir = CodeFileSaverExecutor.executeSaver(parserResult, codeGenType, app.getId(),
                        version);
                log.info("保存成功，路径为：{}", saveDir.getAbsolutePath());
            } catch (Exception e) {
                log.error("保存失败：{}", e.getMessage());
            }
        });
    }
}
