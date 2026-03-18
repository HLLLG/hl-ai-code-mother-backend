package com.hl.hlaicodemother.exception;

/**
 * 抛出异常的工具类
 */
public class ThrowUtils {

    public static void throwIf(boolean condition, RuntimeException exception){
        if (condition){
            throw exception;
        }
    }

    /**
     * 如果条件满足，则抛出业务异常
     * @param condition
     * @param code
     * @param message
     */
    public static void throwIf(boolean condition, int code, String message) {
        throwIf(condition, new BusinessException(code, message));
    }

    /**
     * 如果条件满足，则抛出业务异常
     * @param condition
     * @param errorCode
     */
    public static void throwIf(boolean condition, ErrorCode errorCode) {
        throwIf(condition, new BusinessException(errorCode));
    }

    /**
     * 如果条件满足，则抛出业务异常
     * @param condition
     * @param errorCode
     * @param message
     */
    public static void throwIf(boolean condition, ErrorCode errorCode, String message) {
        throwIf(condition, new BusinessException(errorCode, message));
    }
}
