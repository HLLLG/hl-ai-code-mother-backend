package com.hl.hlaicodemother.model.dto.app;

import lombok.Data;

import java.io.Serializable;

/**
 * 应用创建请求
 */
@Data
public class AppAddRequest implements Serializable {

    private static final long serialVersionUID = 1L;

//    /**
//     * 应用名称
//     */
//    private String appName;
//
//    /**
//     * 应用封面
//     */
//    private String cover;

    /**
     * 应用初始化 prompt
     */
    private String initPrompt;

//    /**
//     * 代码生成类型
//     */
//    private String codeGenType;
}

