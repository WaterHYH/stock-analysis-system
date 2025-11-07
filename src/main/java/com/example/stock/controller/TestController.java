package com.example.stock.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试控制器
 * 提供简单的测试接口，用于验证应用程序是否正常运行
 */
@RestController
public class TestController {

    /**
     * 测试接口
     * 用于验证数据库连接是否正常
     * @return 测试成功消息
     */
    @GetMapping("/test")
    public String test() {
        return "Database connection successful! ";
    }
}