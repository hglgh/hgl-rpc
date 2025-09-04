package com.hgl.hglrpc.loadbalancer;

import com.hgl.hglrpc.spi.SpiLoader;

/**
 * @ClassName: LoadBalancerFactory
 * @Package: com.hgl.hglrpc.loadbalancer
 * @Description: 负载均衡器工厂（工厂模式，用于获取负载均衡器对象）
 * @Author HGL
 * @Create: 2025/9/4 17:55
 */
public class LoadBalancerFactory {
    static {
        SpiLoader.load(LoadBalancer.class);
    }

    /**
     * 默认负载均衡器
     */
    private static final LoadBalancer DEFAULT_LOAD_BALANCER = new RoundRobinLoadBalancer();

    /**
     * 获取实例
     *
     * @param key 键
     * @return 实例
     */
    public static LoadBalancer getInstance(String key) {
        return SpiLoader.getInstance(LoadBalancer.class, key);
    }
}
