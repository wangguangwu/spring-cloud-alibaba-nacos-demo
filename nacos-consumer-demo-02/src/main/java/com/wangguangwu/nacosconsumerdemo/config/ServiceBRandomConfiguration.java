package com.wangguangwu.nacosconsumerdemo.config;

import com.wangguangwu.nacosconsumerdemo.constant.ServiceConstants;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.RandomLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RandomLoadBalancer：随机策略，随机选择一个服务实例来处理请求。
 *
 * @author wangguangwu
 */
@Configuration
@LoadBalancerClient(name = ServiceConstants.SERVICE_B_NAME, configuration = ServiceBRandomConfiguration.class)
public class ServiceBRandomConfiguration {

    @Bean
    public RandomLoadBalancer randomLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider) {
        return new RandomLoadBalancer(serviceInstanceListSupplierProvider, ServiceConstants.SERVICE_B_NAME);
    }
}
