package com.wangguangwu.nacosconsumerdemo.manager;

import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 连接计数器管理类，用于管理和维护每个服务实例的当前连接数。
 * <p>
 * 该类确保在多实例情况下，连接计数的一致性和线程安全性。
 *
 * @author wangguangwu
 */
@Component
@Scope("singleton")
public class ConnectionCounterManager {

    /**
     * 记录每个服务实例的当前连接数
     */
    private final ConcurrentHashMap<String, AtomicInteger> connectionCounts = new ConcurrentHashMap<>();

    /**
     * 初始化指定服务实例的连接计数器。
     * 如果实例的计数器不存在，则将其初始化为 0。
     *
     * @param instances 要初始化计数器的服务实例
     */
    public void init(List<ServiceInstance> instances) {
        instances.forEach(serviceInstance -> connectionCounts.putIfAbsent(getInstanceKey(serviceInstance), new AtomicInteger(0)));
    }

    /**
     * 增加指定服务实例的连接计数。
     *
     * @param instance 要增加连接计数的服务实例
     */
    public void incrementConnectionCount(ServiceInstance instance) {
        connectionCounts.computeIfAbsent(getInstanceKey(instance), key -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     * 减少指定服务实例的连接计数。
     *
     * @param instance 要减少连接计数的服务实例
     */
    public void decrementConnectionCount(ServiceInstance instance) {
        connectionCounts.computeIfPresent(getInstanceKey(instance), (key, count) -> {
            count.decrementAndGet();
            return count;
        });
    }

    /**
     * 比较两个服务实例的连接数。
     * 返回负数表示 instance1 的连接数小于 instance2，返回正数表示连接数更大，返回 0 表示相等。
     *
     * @param instance1 第一个服务实例
     * @param instance2 第二个服务实例
     * @return 比较结果
     */
    public int compare(ServiceInstance instance1, ServiceInstance instance2) {
        return Integer.compare(
                connectionCounts.getOrDefault(getInstanceKey(instance1), new AtomicInteger(0)).get(),
                connectionCounts.getOrDefault(getInstanceKey(instance2), new AtomicInteger(0)).get()
        );
    }

    /**
     * 获取服务实例的唯一标识符
     */
    private String getInstanceKey(ServiceInstance instance) {
        // 例如，使用服务实例的 host 和 port 作为唯一标识符
        return instance.getHost() + ":" + instance.getPort();
    }
}
