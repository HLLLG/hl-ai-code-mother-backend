package com.hl.hlaicodemother.core.saver;


import com.hl.hlaicodemother.ai.model.HtmlCodeResult;
import com.hl.hlaicodemother.ai.model.MultiFileCodeResult;
import com.hl.hlaicodemother.exception.BusinessException;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.model.enums.CodeGenTypeEnum;

import java.io.File;

/**
 * 代码文件保存器执行器（执行器模式）
 */
public class CodeFileSaverExecutor {

    private static final HtmlCodeFileSaver htmlCodeFileSaver = new HtmlCodeFileSaver();

    private static final MultiFileCodeFileSaver multiFileCodeFileSaver = new MultiFileCodeFileSaver();

    /**
     * 执行代码保存器，根据代码生成类型选择合适的保存器进行保存
     *
     * @param codeResult
     * @param codeGenTypeEnum
     * @return
     */
    public static File executeSaver(Object codeResult, CodeGenTypeEnum codeGenTypeEnum, Long appId, int version) {
        return switch (codeGenTypeEnum) {
            case HTML -> htmlCodeFileSaver.saveCode((HtmlCodeResult) codeResult, appId, version);
            case MULTI_FILE -> multiFileCodeFileSaver.saveCode((MultiFileCodeResult) codeResult, appId, version);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenTypeEnum);
        };
    }
}
