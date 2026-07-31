package com.zhixue.ai.common.utils;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

import java.util.List;

/**
 * JSON 工具类(基于 FastJSON2)
 */
public class JsonUtils {

    public static String toJson(Object obj) {
        return obj == null ? null : JSON.toJSONString(obj);
    }

    public static JSONObject parseObject(String json) {
        return JSON.parseObject(json);
    }

    public static JSONArray parseArray(String json) {
        return JSON.parseArray(json);
    }

    public static <T> T parseObject(String json, Class<T> clazz) {
        return JSON.parseObject(json, clazz);
    }

    public static <T> List<T> parseArray(String json, Class<T> clazz) {
        return JSON.parseArray(json, clazz);
    }
}
