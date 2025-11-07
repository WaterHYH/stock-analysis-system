package com.example.stock.utils;

/**
 * 字符串工具类
 * 提供字符串处理相关的工具方法
 */
public class MyStringUtil {
    /**
     * SQL字符串转义方法
     * 将单引号转义为两个单引号，防止SQL注入
     * @param input 原始字符串
     * @return 转义后的字符串
     */
    public static String escapeSqlString(String input) {
        return input.replace("'", "''"); // 单引号转义为两个单引号
    }
}

