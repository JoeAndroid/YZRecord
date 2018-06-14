package com.yanzhi.record.network.utils;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by shenxiaolei on 16/11/23.
 */
public class GsonUtil {
//    private static Gson gson = null;
//
//    static {
//        if (gson == null) {
//            gson = new Gson();
//        }
//    }

    private GsonUtil() {
    }

    /**
     * 转成json
     *
     * @param object
     * @return
     */
    public static String objectToJson(Object object) {
        String gsonString = null;
        Gson gson = new GsonBuilder().serializeNulls().create();
        if (gson != null) {
            gsonString = gson.toJson(object);
        }
        return gsonString;
    }


    /**
     * 转成bean
     *
     * @param gsonString
     * @param cls
     * @return
     */
    public static <T> T jsonToBean(String gsonString, Class<T> cls) {
        T t = null;
        Gson gson = new GsonBuilder().serializeNulls().create();
        if (gson != null) {
            t = gson.fromJson(gsonString, cls);
        }
        return t;
    }

//    /**
//     * 转成list
//     * 泛型在编译期类型被擦除导致报错
//     *
//     * @param gsonString
//     * @param cls
//     * @return
//     */
//    public static <T> List<T> jsonToList(String gsonString, Class<T> cls) {
//        List<T> list = null;
//        Gson gson = new GsonBuilder().serializeNulls().create();
//        if (gson != null) {
//            list = gson.fromJson(gsonString, new TypeToken<List<T>>() {
//            }.getType());
//        }
//        return list;
//    }

    /**
     * 转成list
     * 解决泛型问题
     *
     * @param json
     * @param cls
     * @param <T>
     * @return
     */
    public static <T> List<T> jsonToList(String json, Class<T> cls) {
        Gson gson = new GsonBuilder().serializeNulls().create();
        List<T> list = new ArrayList<T>();
        JsonArray array = new JsonParser().parse(json).getAsJsonArray();
        for (final JsonElement elem : array) {
            list.add(gson.fromJson(elem, cls));
        }
        return list;
    }


    /**
     * 转成list中有map的
     *
     * @param gsonString
     * @return
     */
    public static <T> List<Map<String, T>> jsonToListMaps(String gsonString) {
        List<Map<String, T>> list = null;
        Gson gson = new GsonBuilder().serializeNulls().create();
        if (gson != null) {
            list = gson.fromJson(gsonString,
                    new TypeToken<List<Map<String, T>>>() {
                    }.getType());
        }
        return list;
    }

    /**
     * 转成map的
     *
     * @param gsonString
     * @return
     */
    public static <T> Map<String, T> jsonToMaps(String gsonString) {
        Map<String, T> map = null;
        Gson gson = new GsonBuilder().serializeNulls().create();
        if (gson != null) {
            map = gson.fromJson(gsonString, new TypeToken<Map<String, T>>() {
            }.getType());
        }
        return map;
    }
}
