package com.hl.hlaicodemother.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 分页请求包装类
 */
@Data
public class PageRequest implements Serializable {

    private static final long serialVersionUID = -5283210967686413315L;

    /**
     * 当前页码，默认为1
     */
    private long pageNum = 1;

    /**
     * 每页条数，默认为10
     */
    private long pageSize = 10;

    /**
     * 排序字段，默认为空
     */
    private String sortField;

    /**
     * 排序方式，默认为descend
     */
    private String sortOrder = "descend";
}
