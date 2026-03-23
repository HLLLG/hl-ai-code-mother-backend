package com.hl.hlaicodemother.core.parser;

import com.github.xiaoymin.knife4j.core.util.StrUtil;
import com.hl.hlaicodemother.exception.BusinessException;
import com.hl.hlaicodemother.exception.ErrorCode;
import com.hl.hlaicodemother.exception.ThrowUtils;
import com.hl.hlaicodemother.model.enums.CodeGenTypeEnum;

/**
 * 代码解析器执行器（执行器模式）
 * 负责根据不同的代码类型选择合适的解析器进行解析
 *
 * @author hlllg
 */
public class CodeParserExecutor {

    private static final HtmlCodeParser htmlCodeParser = new HtmlCodeParser();

    private static final MultiFileCodeParser multiFileCodeParser = new MultiFileCodeParser();

    public static Object executeParser(String content, CodeGenTypeEnum codeGenTypeEnum) {
        ThrowUtils.throwIf(StrUtil.isBlank(content), ErrorCode.PARAMS_ERROR, "代码内容不能为空");
        return switch (codeGenTypeEnum) {
            case HTML -> htmlCodeParser.parseCode(content);
            case MULTI_FILE -> multiFileCodeParser.parseCode(content);
            default -> throw new BusinessException(ErrorCode.SYSTEM_ERROR, "不支持的代码生成类型: " + codeGenTypeEnum);
        };
    }
}
