package com.hl.hlaicodemother.core.parser;

import java.util.regex.Pattern;

/**
 * 代码解析器接口（策略模式）
 * 定义了代码解析的基本方法
 *
 * @author hlllg
 */
public interface CodeParser<T> {

    /**
     * 解析代码内容，返回解析结果对象
     * @param content
     * @return
     */
    T parseCode(String content);

    /**
     * 根据正则表达式提取代码块内容
     * @param content
     * @param pattern
     * @return
     */
    String extractCode(String content, Pattern pattern);
}
