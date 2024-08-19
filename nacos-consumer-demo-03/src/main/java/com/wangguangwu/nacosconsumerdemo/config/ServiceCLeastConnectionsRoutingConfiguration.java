package com.wangguangwu.nacosconsumerdemo.config;

import com.wangguangwu.nacosconsumerdemo.constant.ServiceConstants;
import com.wangguangwu.nacosconsumerdemo.loadBalancer.LeastConnectionsRoutingLoadBalancer;
import com.wangguangwu.nacosconsumerdemo.manager.ConnectionCounterManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 配置 {@link LeastConnectionsRoutingLoadBalancer} 作为自定义负载均衡策略，
 * 基于最小连接数选择服务实例，用于 {@link ServiceConstants#SERVICE_C_NAME} 服务。
 * <p>
 * 此配置类通过 {@link LoadBalancerClient} 将自定义的负载均衡器绑定到指定的服务名。
 * 当通过 RestTemplate 或 WebClient 调用服务时，会优先选择连接数最少的服务实例来处理请求，
 * 以实现更均衡的负载分布。
 *
 * @author wangguangwu
 */
@Configuration
@LoadBalancerClient(name = ServiceConstants.SERVICE_C_NAME, configuration = ServiceCLeastConnectionsRoutingConfiguration.class)
public class ServiceCLeastConnectionsRoutingConfiguration {

    /**
     * 注册自定义的 {@link LeastConnectionsRoutingLoadBalancer}，用于在负载均衡时选择
     * 连接数最少的服务实例。
     *
     * @param serviceInstanceListSupplierProvider 提供服务实例列表的供应者
     * @param connectionCounterManager            连接计数管理器
     * @return 自定义的 {@link LeastConnectionsRoutingLoadBalancer} 实例
     */
    @Bean
    public LeastConnectionsRoutingLoadBalancer leastConnectionsRoutingLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
                                                                                   ConnectionCounterManager connectionCounterManager) {
        return new LeastConnectionsRoutingLoadBalancer(serviceInstanceListSupplierProvider, ServiceConstants.SERVICE_C_NAME, connectionCounterManager);
    }
}
