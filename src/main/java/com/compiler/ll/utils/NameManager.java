package com.compiler.ll.utils;

import java.util.HashMap;
import java.util.Map;

public class NameManager {
    private int unnamedCount = 0;
    private final Map<String, Integer> nameCountMap = new HashMap<>();

    /**
     * 获取一个匿名变量的唯一名字，如 %0, %1, %2...
     */
    public String getUniqueName() {
        return "" + (unnamedCount++);
    }

    /**
     * 获取一个基于 hint 的唯一变量名，如 %a, %a.1, %a.2...
     */
    public String getUniqueName(String varName) {
        if (!nameCountMap.containsKey(varName)) {
            nameCountMap.put(varName, 0);
            return varName;
        } else {
            int count = nameCountMap.get(varName) + 1;
            nameCountMap.put(varName, count);
            String newName = varName + "." + count;
            nameCountMap.put(newName, 0);
            return newName;
        }
    }


    public void reset() {
        unnamedCount = 0;
        nameCountMap.clear();
    }
}
