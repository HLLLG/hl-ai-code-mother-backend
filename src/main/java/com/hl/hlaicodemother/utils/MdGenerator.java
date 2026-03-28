package com.hl.hlaicodemother.utils;

import cn.hutool.core.io.FileUtil;
import org.commonmark.node.*;
import org.commonmark.renderer.markdown.MarkdownRenderer;

import java.nio.charset.StandardCharsets;

public class MdGenerator {

    public static void generateMd() {
        // 1. 构建 AST
        Node doc = new Document();

        Heading h1 = new Heading();
        h1.setLevel(1);
        h1.appendChild(new Text("消息报告"));
        doc.appendChild(h1);

        Paragraph p = new Paragraph();
        p.appendChild(new Text("系统于 2026-03-28 生成："));
        p.appendChild(new SoftLineBreak());
        p.appendChild(new Text("消息 ID: 12345 | 状态: 成功"));
        doc.appendChild(p);

        // 2. 渲染为 Markdown 字符串
        MarkdownRenderer renderer = MarkdownRenderer.builder().build();
        String md = renderer.render(doc);

        // 3. 写入文件
        FileUtil.writeString(md, "report.md", StandardCharsets.UTF_8);
    }
}