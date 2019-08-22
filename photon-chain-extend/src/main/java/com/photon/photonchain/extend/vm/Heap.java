package com.photon.photonchain.extend.vm;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author:Lin
 * @Description:
 * @Date:16:46 2018/4/11
 * @Modified by:
 */
public class Heap {
    private Map<String, Object> elementMap = new HashMap<>();

    public void putItem(String key, Object o) {
        elementMap.put(key, o);
    }

    public Object getItem(String key) {
        return elementMap.get(key);
    }
}
