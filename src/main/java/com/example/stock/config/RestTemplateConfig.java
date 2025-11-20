package com.example.stock.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * RestTemplate配置类
 * 配置用于HTTP请求的RestTemplate实例，优化超时设置
 */
@Slf4j
@Configuration
public class RestTemplateConfig {

    /**
     * 配置RestTemplate实例，用于发送HTTP请求
     * 优化点：
     * 1. 设置合理的超时时间，防止请求阻塞
     * 2. 添加请求计时日志
     * @return RestTemplate实例
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        // 使用RestTemplateBuilder配置超时
        RestTemplate restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))  // 连接超时：5秒
                .setReadTimeout(Duration.ofSeconds(15))    // 读取超时：15秒
                .build();
        
        // 替换StringHttpMessageConverter以支持GBK编码
        restTemplate.getMessageConverters().stream()
                .filter(converter -> converter instanceof StringHttpMessageConverter)
                .forEach(converter -> ((StringHttpMessageConverter) converter).setDefaultCharset(StandardCharsets.UTF_8));
        
        // 添加GBK支持的转换器
        StringHttpMessageConverter gbkConverter = new StringHttpMessageConverter();
        gbkConverter.setDefaultCharset(StandardCharsets.UTF_8);
        restTemplate.getMessageConverters().add(0, gbkConverter);
        
        // 添加请求计时拦截器
        restTemplate.getInterceptors().add((request, body, execution) -> {
            long start = System.currentTimeMillis();
            ClientHttpResponse response = execution.execute(request, body);
            long duration = System.currentTimeMillis() - start;
            log.debug("HTTP请求: {} - 状态:{}, 耗时:{}ms", 
                    request.getURI(), response.getStatusCode(), duration);
            return response;
        });
        
        return restTemplate;
    }
}

