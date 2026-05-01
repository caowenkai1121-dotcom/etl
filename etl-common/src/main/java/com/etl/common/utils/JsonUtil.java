package com.etl.common.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import cn.hutool.core.util.StrUtil;

/**
 * JSON工具类
 */
public class JsonUtil {

    private JsonUtil() {}

    /**
     * 对象转JSON字符串
     */
    public static String toJsonString(Object obj) {
        if (obj == null) {
            return null;
        }
        return JSON.toJSONString(obj);
    }

    /**
     * JSON字符串转对象
     */
    public static <T> T parseObject(String json, Class<T> clazz) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return JSON.parseObject(json, clazz);
    }

    /**
     * JSON字符串转JSONObject
     */
    public static JSONObject parseObject(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return JSON.parseObject(json);
    }

    /**
     * JSON字符串转JSONArray
     */
    public static JSONArray parseArray(String json) {
        if (StrUtil.isBlank(json)) {
            return null;
        }
        return JSON.parseArray(json);
    }
}
