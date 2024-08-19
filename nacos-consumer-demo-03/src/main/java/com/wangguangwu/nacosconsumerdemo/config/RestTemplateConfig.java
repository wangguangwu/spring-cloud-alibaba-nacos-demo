package com.wangguangwu.nacosconsumerdemo.config;

import com.wangguangwu.nacosconsumerdemo.interceptor.ConnectionCountingInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.List;

/**
 * RestTemplate 配置类。
 *
 * @author wangguangwu
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 自定义的连接计数拦截器，用于在每次请求时记录和管理连接计数。
     */
    @Resource
    private ConnectionCountingInterceptor connectionCountingInterceptor;

    @LoadBalanced
    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // 配置拦截器
        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
        interceptors.add(connectionCountingInterceptor);
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }
}
