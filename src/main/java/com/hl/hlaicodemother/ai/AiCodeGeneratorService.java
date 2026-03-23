package com.hl.hlaicodemother.ai;

import com.hl.hlaicodemother.ai.model.HtmlCodeResult;
import com.hl.hlaicodemother.ai.model.MultiFileCodeResult;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import reactor.core.publisher.Flux;

/**
 * Ai Service接口，定义了生成HTML代码和生成多文件代码的方法。每个方法都使用了SystemMessage注解来指定系统提示信息的来源资源文件。
 */
@AiService
public interface AiCodeGeneratorService {

    /**
     * 生成HTML代码的方法，
     * 接受一个用户消息作为输入，并返回生成的HTML代码。
     * 系统提示信息从"prompts/codegen-html-system-prompt.txt"资源文件中获取。
     * @param userMessage
     * @return
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    HtmlCodeResult generateHtmlCode(String userMessage);

    /**
     * 生成多文件代码的方法，接受一个用户消息作为输入，并返回生成的多文件代码。
     * 系统提示信息从"prompts/codegen-multi-file-system-prompt.txt"资源文件中获取。
     * @param userMessage
     * @return
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    MultiFileCodeResult generateMultiFileCode(String userMessage);

    /**
     * 生成HTML代码的方法，（流式）
     * 接受一个用户消息作为输入，并返回生成的HTML代码。
     * 系统提示信息从"prompts/codegen-html-system-prompt.txt"资源文件中获取。
     * @param userMessage
     * @return
     */
    @SystemMessage(fromResource = "prompt/codegen-html-system-prompt.txt")
    Flux<String> generateHtmlCodeStream(String userMessage);

    /**
     * 生成多文件代码的方法，接受一个用户消息作为输入，并返回生成的多文件代码。（流式）
     * 系统提示信息从"prompts/codegen-multi-file-system-prompt.txt"资源文件中获取。
     * @param userMessage
     * @return
     */
    @SystemMessage(fromResource = "prompt/codegen-multi-file-system-prompt.txt")
    Flux<String> generateMultiFileStream(String userMessage);
}