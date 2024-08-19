# 自定义 Spring Cloud Balancer 策略

### 文章地址

[自定义 Spring Cloud Balancer 策略](https://www.wangguangwu.com/archives/5039dc7d-5d88-4b67-a626-47682cedc328)

# 1. 默认策略和常见策略

Spring Cloud Balancer 中提供了两种负载均衡策略。

- 轮询策略（Round Robin）
- 随机策略（Random）

除了这两种策略之外，还有一些常见的负载均衡策略。以下是几种常见的负载均衡策略：

## 1.1 轮询策略（Round Robin）

- **描述**: 将请求均匀地分配给每个服务器，轮流处理请求。
- **适用场景**: 适用于负载较为均衡的服务场景。

## 1.2 随机策略（Random）

- **描述**: 随机选择服务器来处理请求，避免请求集中在少数服务器上。
- **适用场景**: 适用于希望随机分配请求以避免负载过于集中的场景。

## 1.3 最小连接数策略（Least Connections）

- **描述**: 选择当前处理连接数最少的服务器。这种策略确保了服务器的负载是基于实际连接数分配的。
- **适用场景**: 适用于长连接场景，比如需要处理大量并发连接的服务器，如数据库服务器、WebSocket 连接等。

## 1.4 最小响应时间策略（Least Response Time）

- **描述**: 选择响应时间最短的服务器。这种策略根据服务器的响应速度动态选择服务器。
- **适用场景**: 适用于希望提高系统响应速度的场景，特别是在服务器性能差异较大或需要动态适应负载的情况下。

## 1.5 加权轮询策略（Weighted Round Robin）

- **描述**: 在轮询策略的基础上，为每个服务器分配一个权重，权重越高的服务器分配到的请求越多。
- **适用场景**: 适用于服务器性能不一致的场景，可以根据服务器的处理能力分配不同的请求负载。

## 1.6 加权最小连接数策略（Weighted Least Connections）

- **描述**: 在最小连接数策略的基础上，为服务器分配权重，权重较高的服务器会分配更多的连接。
- **适用场景**: 类似于最小连接数策略，但可以根据服务器的负载能力动态调整连接分配。

## 1.7 哈希策略（Consistent Hashing）

- **描述**: 根据请求的某个特定属性（如客户端 IP、请求 URL 等）计算哈希值，并根据哈希值选择服务器。常用于会话保持或粘性会话场景。
- **适用场景**: 适用于需要将同一客户端请求路由到同一服务器的场景，如需要会话保持或缓存一致性的应用。

## 1.8 IP Hash（IP 哈希）

- **描述**: 基于请求的源 IP 地址进行哈希计算，将请求路由到相应的服务器。此策略保证了同一 IP 地址的请求始终路由到同一台服务器。
- **适用场景**: 适用于需要将来自同一客户端 IP 的请求总是路由到同一服务器的场景。

## 1.9 区域感知策略（Zone-Aware Load Balancing）

- **描述**: 选择与客户端在同一可用区域（zone）内的服务器，减少跨区域的网络延迟。
- **适用场景**: 适用于跨多个数据中心或多个可用区域部署的系统，确保请求优先分配到同一区域的服务器。

## 1.10 总结

- **轮询策略**: 适用于均匀分配请求的场景，适合负载较为均衡的服务。
- **随机策略**: 适用于希望随机分配请求以避免负载过于集中的场景。
- **最小连接数策略**: 适用于长连接和高并发场景，如数据库连接、WebSocket。
- **最小响应时间策略**: 适用于需要动态适应服务器性能差异、提高响应速度的场景。
- **加权轮询/最小连接数策略**: 适用于服务器性能不一致的场景，根据服务器处理能力分配负载。
- **哈希策略**: 适用于需要粘性会话或一致性路由的场景。

这些策略可以根据不同的业务需求和系统架构选择，确保系统的稳定性和高效性。

# 2. 代码实现

## 2.1 自定义策略

实现 org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer 接口，以实现自定义的**最小连接数策略**。

### 2.1.1 最小连接数策略

**LeastConnectionsRoutingLoadBalancer**：

```java
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
 - 自定义负载均衡策略实现，基于最小连接数选择服务实例。
 - <p>
 - 参考 {@link RoundRobinLoadBalancer} 的轮询策略实现。
 - 在高并发环境下，通过追踪每个服务实例的当前连接数，
 - 将请求分配给连接数最少的服务实例，以实现负载均衡。
 *
 - @author wangguangwu
 */
public class LeastConnectionsRoutingLoadBalancer implements ReactorServiceInstanceLoadBalancer {

    private static final Log log = LogFactory.getLog(LeastConnectionsRoutingLoadBalancer.class);

    /**
     - 服务 Id，用于标识当前的服务实例
     */
    private final String serviceId;

    /**
     - 提供服务实例列表的供应者
     */
    private final ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider;

    /**
     - 连接计数器管理类
     */
    private final ConnectionCounterManager connectionCounterManager;

    /**
     - 构造函数，初始化负载均衡器。
     *
     - @param serviceInstanceListSupplierProvider 服务实例列表的供应者
     - @param serviceId                           服务标识符
     - @param connectionCounterManager            连接计数器管理类
     */
    public LeastConnectionsRoutingLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
                                               String serviceId,
                                               ConnectionCounterManager connectionCounterManager) {
        this.serviceInstanceListSupplierProvider = serviceInstanceListSupplierProvider;
        this.serviceId = serviceId;
        this.connectionCounterManager = connectionCounterManager;
    }

    /**
     - 选择一个连接数最少的服务实例。
     *
     - @param request 当前的负载均衡请求
     - @return 包含选择的服务实例的响应
     */
    @Override
    public Mono<Response<ServiceInstance>> choose(Request request) {
        ServiceInstanceListSupplier supplier = serviceInstanceListSupplierProvider
                .getIfAvailable(NoopServiceInstanceListSupplier::new);
        return supplier.get(request).next()
                .map(serviceInstances -> processInstanceResponse(supplier, serviceInstances));
    }

    /**
     - 处理服务实例列表，返回连接最少的服务实例。
     *
     - @param supplier         服务实例列表供应者
     - @param serviceInstances 服务实例列表
     - @return 包含选择的服务实例的响应
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
     - 从服务实例列表中选择连接数量最少的实例。
     *
     - @param instances 服务实例列表
     - @return 包含选择的服务实例的响应
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
```

### 2.1.2 连接计数器管理类

**ConnectionCounterManager**:

```java
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 - 连接计数器管理类，用于管理和维护每个服务实例的当前连接数。
 - <p>
 - 该类确保在多实例情况下，连接计数的一致性和线程安全性。
 *
 - @author wangguangwu
 */
@Component
@Scope("singleton")
public class ConnectionCounterManager {

    /**
     - 记录每个服务实例的当前连接数
     */
    private final ConcurrentHashMap<String, AtomicInteger> connectionCounts = new ConcurrentHashMap<>();

    /**
     - 初始化指定服务实例的连接计数器。
     - 如果实例的计数器不存在，则将其初始化为 0。
     *
     - @param instances 要初始化计数器的服务实例
     */
    public void init(List<ServiceInstance> instances) {
        instances.forEach(serviceInstance -> connectionCounts.putIfAbsent(getInstanceKey(serviceInstance), new AtomicInteger(0)));
    }

    /**
     - 增加指定服务实例的连接计数。
     *
     - @param instance 要增加连接计数的服务实例
     */
    public void incrementConnectionCount(ServiceInstance instance) {
        connectionCounts.computeIfAbsent(getInstanceKey(instance), key -> new AtomicInteger(0)).incrementAndGet();
    }

    /**
     - 减少指定服务实例的连接计数。
     *
     - @param instance 要减少连接计数的服务实例
     */
    public void decrementConnectionCount(ServiceInstance instance) {
        connectionCounts.computeIfPresent(getInstanceKey(instance), (key, count) -> {
            count.decrementAndGet();
            return count;
        });
    }

    /**
     - 比较两个服务实例的连接数。
     - 返回负数表示 instance1 的连接数小于 instance2，返回正数表示连接数更大，返回 0 表示相等。
     *
     - @param instance1 第一个服务实例
     - @param instance2 第二个服务实例
     - @return 比较结果
     */
    public int compare(ServiceInstance instance1, ServiceInstance instance2) {
        return Integer.compare(
                connectionCounts.getOrDefault(getInstanceKey(instance1), new AtomicInteger(0)).get(),
                connectionCounts.getOrDefault(getInstanceKey(instance2), new AtomicInteger(0)).get()
        );
    }

    /**
     - 获取服务实例的唯一标识符
     */
    private String getInstanceKey(ServiceInstance instance) {
        // 例如，使用服务实例的 host 和 port 作为唯一标识符
        return instance.getHost() + ":" + instance.getPort();
    }
}
```

## 2.2 自定义拦截器

### 2.2.1 连接拦截器

**ConnectionCountingInterceptor**：

```java
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
 - 自定义拦截器，适配 {@link LeastConnectionsRoutingLoadBalancer 自定义最小连接数策略}，
 - 在请求前后管理连接记录。
 - <p>
 - 根据请求的服务名动态选择适当的 LoadBalancer 进行服务实例的选择。
 *
 - @author wangguangwu
 */
@Component
public class ConnectionCountingInterceptor implements ClientHttpRequestInterceptor {

    /**
     - 负载均衡客户端，用于选择服务实例
     */
    @Resource
    private LoadBalancerClient loadBalancerClient;

    /**
     - 连接计数器管理类
     */
    @Resource
    private ConnectionCounterManager connectionCounterManager;

	/**  
	 * 负载均衡器实例管理器  
	 */
    @Resource
    private LoadBalancerManager loadBalancerManager;

    /**
     - 拦截 HTTP 请求，根据请求的服务名动态选择服务实例，并管理连接计数。
     *
     - @param request   当前的 HTTP 请求
     - @param body      请求体
     - @param execution 请求执行器
     - @return HTTP 响应
     - @throws IOException 如果请求执行失败
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

        // 增加连接计数，如果该服务使用了 LeastConnectionsRoutingLoadBalancer        if (isUsingLeastConnectionsRoutingLoadBalancer(serviceName)) {
            connectionCounterManager.incrementConnectionCount(serviceInstance);
        }

        ClientHttpResponse response;
        try {
            // 发出请求
            response = execution.execute(request, body);
        } finally {
            // 减少连接数，如果该服务使用了 LeastConnectionsRoutingLoadBalancer            if (isUsingLeastConnectionsRoutingLoadBalancer(serviceName)) {
                connectionCounterManager.decrementConnectionCount(serviceInstance);
            }
        }
        return response;
    }

    /**
     - 检查指定的服务是否使用了 LeastConnectionsRoutingLoadBalancer。
     *
     - @param serviceName 服务名
     - @return 如果服务使用了 LeastConnectionsRoutingLoadBalancer，返回 true，否则返回 false。
     */
    private boolean isUsingLeastConnectionsRoutingLoadBalancer(String serviceName) {
        return loadBalancerManager.getLoadBalancer(serviceName) instanceof LeastConnectionsRoutingLoadBalancer;
    }
}
```

### 2.2.2 负载均衡器实例管理器

**LoadBalancerManager**:

```java
import org.springframework.cloud.loadbalancer.core.ReactorServiceInstanceLoadBalancer;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.concurrent.ConcurrentHashMap;

/**
 - 负载均衡器实例管理器。
 *
 - @author wangguangwu
 */
@Component
public class LoadBalancerManager {

    /**
     - 缓存已获取的负载均衡器实例
     */
    private final ConcurrentHashMap<String, ReactorServiceInstanceLoadBalancer> loadBalancerCache = new ConcurrentHashMap<>();

    /**
     - 负载均衡器工厂，用于获取负载均衡器实例
     */
    @Resource
    private LoadBalancerClientFactory loadBalancerClientFactory;

    /**
     - 从缓存或通过工厂获取指定服务的负载均衡器实例。
     *
     - @param serviceName 服务名
     - @return 负载均衡器实例
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
```

## 2.3 注册负载均衡策略和拦截器

### 2.3.1 注册负载均衡策略

**ServiceCLeastConnectionsRoutingConfiguration**:

```java
import com.wangguangwu.nacosconsumerdemo.constant.ServiceConstants;
import com.wangguangwu.nacosconsumerdemo.loadBalancer.LeastConnectionsRoutingLoadBalancer;
import com.wangguangwu.nacosconsumerdemo.manager.ConnectionCounterManager;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 - 配置 {@link LeastConnectionsRoutingLoadBalancer} 作为自定义负载均衡策略，
 - 基于最小连接数选择服务实例，用于 {@link ServiceConstants#SERVICE_C_NAME} 服务。
 - <p>
 - 此配置类通过 {@link LoadBalancerClient} 将自定义的负载均衡器绑定到指定的服务名。
 - 当通过 RestTemplate 或 WebClient 调用服务时，会优先选择连接数最少的服务实例来处理请求，
 - 以实现更均衡的负载分布。
 *
 - @author wangguangwu
 */
@Configuration
@LoadBalancerClient(name = ServiceConstants.SERVICE_C_NAME, configuration = ServiceCLeastConnectionsRoutingConfiguration.class)
public class ServiceCLeastConnectionsRoutingConfiguration {

    /**
     - 注册自定义的 {@link LeastConnectionsRoutingLoadBalancer}，用于在负载均衡时选择
     - 连接数最少的服务实例。
     *
     - @param serviceInstanceListSupplierProvider 提供服务实例列表的供应者
     - @param connectionCounterManager            连接计数管理器
     - @return 自定义的 {@link LeastConnectionsRoutingLoadBalancer} 实例
     */
    @Bean
    public LeastConnectionsRoutingLoadBalancer leastConnectionsRoutingLoadBalancer(ObjectProvider<ServiceInstanceListSupplier> serviceInstanceListSupplierProvider,
                                                                                   ConnectionCounterManager connectionCounterManager) {
        return new LeastConnectionsRoutingLoadBalancer(serviceInstanceListSupplierProvider, ServiceConstants.SERVICE_C_NAME, connectionCounterManager);
    }
}
```

### 2.3.2 注册拦截器

**RestTemplateConfig**:

```java
import com.wangguangwu.nacosconsumerdemo.interceptor.ConnectionCountingInterceptor;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.List;

/**
 - RestTemplate 配置类。
 *
 - @author wangguangwu
 */
@Configuration
public class RestTemplateConfig {

    /**
     - 自定义的连接计数拦截器，用于在每次请求时记录和管理连接计数。
     */
    @Resource
    private ConnectionCountingInterceptor connectionCountingInterceptor;

    @LoadBalanced
    @Bean    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        // 配置拦截器
        List<ClientHttpRequestInterceptor> interceptors = restTemplate.getInterceptors();
        interceptors.add(connectionCountingInterceptor);
        restTemplate.setInterceptors(interceptors);
        return restTemplate;
    }
}
```

## 2.4 总结

在本篇文章中，我们详细探讨了 Spring Cloud LoadBalancer 中常见的负载均衡策略，以及如何在具体项目中实现和应用自定义的负载均衡策略。我们首先介绍了默认的轮询和随机策略，以及其他常见的负载均衡策略，如最小连接数策略、最小响应时间策略、加权策略和哈希策略等，并分析了它们各自的适用场景。

随后，我们深入讲解了如何通过实现 `ReactorServiceInstanceLoadBalancer` 接口，创建自定义的最小连接数策略。通过使用 `LeastConnectionsRoutingLoadBalancer` 类，我们能够在高并发环境下，动态选择连接数最少的服务器实例，达到更高效的请求分配。

最后，我们展示了如何通过 `ConnectionCounterManager` 类来管理和维护服务器实例的连接计数，以及如何利用 `ConnectionCountingInterceptor` 拦截器，将自定义策略集成到应用中。通过这些技术和工具的组合使用，开发者可以有效地提升应用的负载均衡能力，确保系统在高并发场景下的稳定性和响应速度。

本篇文章提供的指导和代码示例，可帮助开发者在实际项目中灵活应用各种负载均衡策略，满足不同业务需求，从而实现更高效、更可靠的分布式系统架构。