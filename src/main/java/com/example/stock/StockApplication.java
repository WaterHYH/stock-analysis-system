package com.example.stock;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 股票系统主启动类
 * Spring Boot应用程序入口点
 */
@SpringBootApplication //主类注解
@EnableScheduling //定时任务注解
public class StockApplication {

	/**
	 * 应用程序主方法
	 * 启动Spring Boot应用程序
	 * @param args 命令行参数
	 */
	public static void main(String[] args) {
		SpringApplication.run(StockApplication.class, args);
	}

}
