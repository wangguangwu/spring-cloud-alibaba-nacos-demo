package com.wangguangwu.nacosconsumerdemo.loadBalancer;

import com.wangguangwu.nacosconsumerdemo.manager.ConnectionCounterManager;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.DefaultResponse;
import org.springframework.cloud.client.loadbalancer.EmptyResponse;
import org.springframework.cloud.client.loadbalancer.Request;
import org.springframework.cloud.client.loadbalancer.Response;
import org.springframework.cloud.loadbalancer.core.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 自定义负载均衡策略实现，基于最小连接数选择服务实例。
 * <p>
 * 参考 {@link RoundRobinLoadBalancer} 的轮询策略实现。
 * 在高并发环境下，通过追踪每个服务实例的当前连接数，
 * 将请求分配给连接数最少的服务实例，以实现负载均衡。
 *
 * @author wangguangwu
 */
public class LeastConnectionsRoutingLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private static final Log log = LogFactory.getLog(LeastConnectionsRoutingLoadBalancer.class);

    /**
     * 服务 Id，用于标识当前的服务实例
     */
    private final String serviceId;

    /**
     * 提供服务实例列表的供应者
     */
    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

    /**
     * 连接计数器管理类
     */
    private final ConnectionCounterManager connectionCounterManager;

    /**
     * 构造函数，初始化负载均衡器。
     *
     * @param serviceInstanceListSupplierProvider 服务实例列表的供应者
     * @param serviceId                           服务标识符
     * @param connectionCounterManager            连接计数器管理类
     */
    public LeastConnectionsRoutingLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
                                               String serviceId,
                                               ConnectionCounterManager connectionCounterManager) {
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
        this.serviceId = serviceId;
        this.connectionCounterManager = connectionCounterManager;
    }

    /**
     * 选择一个连接数最少的服务实例。
     *
     * @param request 当前的负载均衡请求
     * @return 包含选择的服务实例的响应
     */
    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
                .getIfAvailable(NoopServiceInstanceListSupplier::new);
        return supplier.get(request).next()
                .map(serviceInstances -> processInstanceResponse(supplier, serviceInstances));
    }

    /**
     * 处理服务实例列表，返回连接最少的服务实例。
     *
     * @param supplier         服务实例列表供应者
     * @param serviceInstances 服务实例列表
     * @return 包含选择的服务实例的响应
     */
    private Response<ServiceInstance> processInstanceResponse(ServiceInstanceListSupplier supplier,
                                                              List<ServiceInstance> serviceInstances) {
        Response<ServiceInstance> serviceInstanceResponse = getInstanceResponse(serviceInstances);
        if (supplier instanceof SelectedInstanceCallback && serviceInstanceResponse.hasServer()) {
            ((SelectedInstanceCallback) supplier).selectedServiceInstance(serviceInstanceResponse.getServer());
        }
        return serviceInstanceResponse;
    }

    /**
     * 从服务实例列表中选择连接数量最少的实例。
     *
     * @param instances 服务实例列表
     * @return 包含选择的服务实例的响应
     */
    private Response<ServiceInstance> getInstanceResponse(List<ServiceInstance> instances) {
        if (instances.isEmpty()) {
            if (log.isWarnEnabled()) {
                log.warn("No servers available for service: " + serviceId);
            }
            return new EmptyResponse();
        }
        // 初始化连接数
        connectionCounterManager.init(instances);

        // 找到连接数最少的实例
        ServiceInstance chosenInstance = instances.stream()
                .min(connectionCounterManager::compare)
                .orElse(instances.get(0));

        return new DefaultResponse(chosenInstance);
    }
}
