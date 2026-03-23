package com.hl.hlaicodemother.core.saver;

import cn.hutool.core.util.StrUtil;
import com.hl.hlaicodemother.ai.model.HtmlCodeResult;
import com.hl.hlaicodemother.model.enums.CodeGenTypeEnum;

/**
 * HTML 代码文件保存器
 * 负责将 AI 生成的 HTML 代码结果保存到本地文件系统中
 */
public class HtmlCodeFileSaver extends CodeFileSaverTemplate<HtmlCodeResult> {

    @Override
    protected CodeGenTypeEnum getCodeType() {
        return CodeGenTypeEnum.HTML;
    }

    @Override
    protected void validateInput(HtmlCodeResult result) {
        super.validateInput(result);
        if (StrUtil.isBlank(result.getHtmlCode())) {
            throw new IllegalArgumentException("HTML 代码内容不能为空");
        }
    }

    @Override
    protected void saveFiles(HtmlCodeResult result, String dirPath) {
        writeToFile(dirPath, "index.html", result.getHtmlCode());
    }


}
