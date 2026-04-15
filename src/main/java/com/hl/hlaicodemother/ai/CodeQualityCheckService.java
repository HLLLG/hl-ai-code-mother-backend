package com.hl.hlaicodemother.ai;

import com.hl.hlaicodemother.langgraph4j.model.QualityResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;

/**
 * 代码质量检查服务
 *
 * @author hl
 */
public interface CodeQualityCheckService {

    @SystemMessage(fromResource = "prompt/codegen-quality-check-system-prompt.txt")
    QualityResult checkCodeQuality(@UserMessage String userMessage);
}
