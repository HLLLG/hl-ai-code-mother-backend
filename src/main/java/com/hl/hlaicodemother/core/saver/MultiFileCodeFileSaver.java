package com.hl.hlaicodemother.core.saver;

import cn.hutool.core.util.StrUtil;
import com.hl.hlaicodemother.ai.model.MultiFileCodeResult;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.exception.ThrowUtils;
import com.hl.hlaicodemother.model.enums.CodeGenTypeEnum;

/**
 * 多文件代码文件保存器
 * 负责将 AI 生成的多文件代码结果（HTML、CSS、JS）保存到本地文件系统中
 */
public class MultiFileCodeFileSaver extends CodeFileSaverTemplate<MultiFileCodeResult> {

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.MULTI_FILE;
    }

    @Override
    protected void validateInput(MultiFileCodeResult result) {
        super.validateInput(result);
        ThrowUtils.throwIf(StrUtil.isBlank(result.getHtmlCode()), ErrorCode.PARAMS_ERROR, "HTML 代码内容不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(result.getCssCode()), ErrorCode.PARAMS_ERROR, "CSS 代码内容不能为空");
        ThrowUtils.throwIf(StrUtil.isBlank(result.getJsCode()), ErrorCode.PARAMS_ERROR, "JS 代码内容不能为空");
    }

    @Override
    protected void saveFiles(MultiFileCodeResult result, String dirPath) {
        writeToFile(dirPath, "index.html", result.getHtmlCode());
        writeToFile(dirPath, "style.css", result.getCssCode());
        writeToFile(dirPath, "script.js", result.getJsCode());
    }
}
