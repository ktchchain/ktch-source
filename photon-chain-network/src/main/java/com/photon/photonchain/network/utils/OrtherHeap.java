package com.photon.photonchain.network.utils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by lqh on 2018/8/9.
 */
public class OrtherHeap {

    private Map<String, Object> elementMap = new HashMap<>();

    public void putItem(String key, Object o) {
        elementMap.put(key, o);
    }

    public Object getItem(String key) {
        return elementMap.get(key);
    }

}
