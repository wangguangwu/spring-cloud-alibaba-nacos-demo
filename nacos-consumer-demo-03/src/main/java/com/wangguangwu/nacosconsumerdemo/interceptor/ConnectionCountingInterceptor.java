package com.wangguangwu.nacosconsumerdemo.interceptor;

import com.wangguangwu.nacosconsumerdemo.loadBalancer.LeastConnectionsRoutingLoadBalancer;
import com.wangguangwu.nacosconsumerdemo.manager.ConnectionCounterManager;
import com.wangguangwu.nacosconsumerdemo.manager.LoadBalancerManager;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;

/**
 * 自定义拦截器，适配 {@link LeastConnectionsRoutingLoadBalancer 自定义最小连接数策略}，
 * 在请求前后管理连接记录。
 * <p>
 * 根据请求的服务名动态选择适当的 LoadBalancer 进行服务实例的选择。
 *
 * @author wangguangwu
 */
@Component
public class ConnectionCountingInterceptor implements ClientHttpRequestInterceptor {

    /**
     * 负载均衡客户端，用于选择服务实例
     */
    @Resource
    private LoadBalancerClient loadBalancerClient;

    /**
     * 连接计数器管理类
     */
    @Resource
    private ConnectionCounterManager connectionCounterManager;

    /**
     * 负载均衡器实例管理器
     */
    @Resource
    private LoadBalancerManager loadBalancerManager;

    /**
     * 拦截 HTTP 请求，根据请求的服务名动态选择服务实例，并管理连接计数。
     *
     * @param request   当前的 HTTP 请求
     * @param body      请求体
     * @param execution 请求执行器
     * @return HTTP 响应
     * @throws IOException 如果请求执行失败
     */
    @Override
    @SuppressWarnings("all")
    public ClientHttpResponse intercept(final HttpRequest request, final byte[] body,
                                        ClientHttpRequestExecution execution) throws IOException {
        // 获取请求的服务名
        String serviceName = request.getURI().getHost();

        // 通过 LoadBalancerClient 动态选择服务实例
        ServiceInstance serviceInstance = loadBalancerClient.choose(serviceName);

        // 检查是否配置了 LeastConnectionsRoutingLoadBalancer 策略
        if (serviceInstance == null) {
            throw new UnsupportedOperationException("No available service instance found for request");
        }

        // 增加连接计数，如果该服务使用了 LeastConnectionsRoutingLoadBalancer
        if (isUsingLeastConnectionsRoutingLoadBalancer(serviceName)) {
            connectionCounterManager.incrementConnectionCount(serviceInstance);
        }

        ClientHttpResponse response;
        try {
            // 发出请求
            response = execution.execute(request, body);
        } finally {
            // 减少连接数，如果该服务使用了 LeastConnectionsRoutingLoadBalancer
            if (isUsingLeastConnectionsRoutingLoadBalancer(serviceName)) {
                connectionCounterManager.decrementConnectionCount(serviceInstance);
            }
        }
        return response;
    }

    /**
     * 检查指定的服务是否使用了 LeastConnectionsRoutingLoadBalancer。
     *
     * @param serviceName 服务名
     * @return 如果服务使用了 LeastConnectionsRoutingLoadBalancer，返回 true，否则返回 false。
     */
    private boolean isUsingLeastConnectionsRoutingLoadBalancer(String serviceName) {
        return loadBalancerManager.getLoadBalancer(serviceName) instanceof LeastConnectionsRoutingLoadBalancer;
    }
}
