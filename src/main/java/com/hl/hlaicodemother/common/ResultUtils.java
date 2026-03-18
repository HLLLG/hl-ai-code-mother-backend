package com.hl.hlaicodemother.common;

import com.hl.hlaicodemother.exception.ErrorCode;

/**
 * 统一响应结果工具类
 */
public class ResultUtils {

    /**
     * 成功响应
     * @param data
     * @return
     * @param <T>
     */
    public static <T> BaseResponse<T> success(T data) {
        return new BaseResponse<>(0, "ok", data);
    }

    /**
     * 错误响应
     * @param code
     * @param message
     * @return
     * @param <T>
     */
    public static <T> BaseResponse<T> error(int code, String message) {
        return new BaseResponse<>(code, message, null);
    }

    /**
     * 错误响应，使用错误码枚举
     * @param errorCode
     * @return
     * @param <T>
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode) {
        return new BaseResponse<>(errorCode);
    }

    /**
     * 错误响应，使用错误码枚举和自定义消息
     * @param errorCode
     * @param message
     * @return
     * @param <T>
     */
    public static <T> BaseResponse<T> error(ErrorCode errorCode, String message) {
        return new BaseResponse<>(errorCode.getCode(), message, null);
    }
}
