package com.wangguangwu.nacosconsumerdemo.manager;

import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 负载均衡器实例管理器。
 *
 * @author wangguangwu
 */
@Component
public class LoadBalancerManager {

    /**
     * 缓存已获取的负载均衡器实例
     */
    private final ConcurrentHashMap<String, ReactorServiceInstanceLoadBalancer> loadBalancerCache = new ConcurrentHashMap<>();

    /**
     * 负载均衡器工厂，用于获取负载均衡器实例
     */
    @Resource
    private LoadBalancerClientFactory loadBalancerClientFactory;

    /**
     * 从缓存或通过工厂获取指定服务的负载均衡器实例。
     *
     * @param serviceName 服务名
     * @return 负载均衡器实例
     */
    public ReactorServiceInstanceLoadBalancer getLoadBalancer(String serviceName) {
        ReactorServiceInstanceLoadBalancer loadBalancer = loadBalancerCache.computeIfAbsent(serviceName, key ->
                loadBalancerClientFactory.getInstance(key, ReactorServiceInstanceLoadBalancer.class));
        if (loadBalancer == null) {
            // 通过 LoadBalancerClientFactory 获取指定服务的负载均衡器类型
            loadBalancer = loadBalancerClientFactory.getInstance(serviceName, ReactorServiceInstanceLoadBalancer.class);
            loadBalancerCache.putIfAbsent(serviceName, loadBalancer);
        }
        return loadBalancer;
    }
}
