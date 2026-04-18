package com.hl.hlaicodemother.utils;

import cn.hutool.crypto.digest.DigestUtil;
import cn.hutool.json.JSONUtil;

/**
 * 缓存key工具类
 *
 * @author hl
 */
public class CacheKeyUtils {

    /**
     * 根据对象生成缓存key
     *
     * @param obj
     * @return
     */
    public static String generateKey(Object obj) {
        if (obj == null) {
            return DigestUtil.md5Hex("null");
        }
        // 先转 JSON 再转 MD5
        String jsonStr = JSONUtil.toJsonStr(obj);
        return DigestUtil.md5Hex(jsonStr);
    }


    /**
     * 生成带业务前缀的缓存key
     *
     * @param prefix 业务前缀，如 "user:", "app:"
     * @param obj 业务对象
     * @return 完整的缓存key
     */
    public static String generateKeyWithPrefix(String prefix, Object obj) {
        return prefix + ":" + generateKey(obj);
    }

}
