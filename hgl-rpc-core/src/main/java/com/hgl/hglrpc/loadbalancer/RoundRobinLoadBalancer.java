package com.hgl.hglrpc.loadbalancer;

import com.hgl.hglrpc.model.ServiceMetaInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @ClassName: RoundRobinLoadBalancer
 * @Package: com.hgl.hglrpc.loadbalancer
 * @Description: 轮询负载均衡器
 * @Author HGL
 * @Create: 2025/9/4 17:44
 */
public class RoundRobinLoadBalancer implements LoadBalancer {
    /**
     * 为每个服务维护一个独立的轮询计数器
     * key: 服务键名 (serviceName:serviceVersion)
     * value: 该服务的轮询索引
     */
    private final Map<String, AtomicInteger> serviceCounterMap = new ConcurrentHashMap<>();

    @Override
    public ServiceMetaInfo select(Map<String, Object> requestParams, List<ServiceMetaInfo> serviceMetaInfoList) {
        if (serviceMetaInfoList.isEmpty()) {
            return null;
        }

        // 只有一个服务，无需轮询
        if (serviceMetaInfoList.size() == 1) {
            return serviceMetaInfoList.get(0);
        }

        // 获取第一个服务的键名作为标识
        String serviceKey = serviceMetaInfoList.get(0).getServiceKey();

        // 获取或创建该服务的计数器
        AtomicInteger counter = serviceCounterMap.computeIfAbsent(
                serviceKey,
                k -> new AtomicInteger(0)
        );

        // 原子递增并取模，确保索引在有效范围内
        int index = Math.abs(counter.getAndIncrement()) % serviceMetaInfoList.size();
        return serviceMetaInfoList.get(index);
    }
}
