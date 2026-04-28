package com.hl.hlaicodemother.common;

import com.hl.hlaicodemother.exception.ErrorCode;
import lombok.Data;

import java.io.Serializable;

/**
 * 统一响应结果类
 * @param <T> 响应数据的类型
 */
@Data
public class BaseResponse<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;

    private String message;

    private T data;

    public BaseResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public BaseResponse(int code, T data) {
        this(code, "", data);
    }

    public BaseResponse(ErrorCode errorCode) {
        this(errorCode.getCode(), errorCode.getMessage(), null);
    }
}
