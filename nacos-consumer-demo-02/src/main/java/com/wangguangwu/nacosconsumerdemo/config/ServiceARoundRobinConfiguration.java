package com.wangguangwu.nacosconsumerdemo.config;

import com.wangguangwu.nacosconsumerdemo.constant.ServiceConstants;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.RoundRobinLoadBalancer;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RoundRobinLoadBalancer：轮询策略，按顺序循环地选择服务实例。
 * <p>
 * 默认的负载均衡策略。
 *
 * @author wangguangwu
 */
@Configuration
@LoadBalancerClient(name = ServiceConstants.SERVICE_A_NAME, configuration = ServiceARoundRobinConfiguration.class)
public class ServiceARoundRobinConfiguration {

    @Bean
    public RoundRobinLoadBalancer roundRobinLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider) {
        return new RoundRobinLoadBalancer(serviceInstanceListSupplierProvider, ServiceConstants.SERVICE_A_NAME);
    }
}
