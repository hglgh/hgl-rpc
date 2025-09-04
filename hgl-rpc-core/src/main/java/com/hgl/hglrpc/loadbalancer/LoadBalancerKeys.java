package com.hgl.hglrpc.loadbalancer;

/**
 * @ClassName: LoadBalancerKeys
 * @Package: com.hgl.hglrpc.loadbalancer
 * @Description: 负载均衡器键名常量
 * @Author HGL
 * @Create: 2025/9/4 17:55
 */
public interface LoadBalancerKeys {
    /**
     * 轮询
     */
    String ROUND_ROBIN = "roundRobin";

    String RANDOM = "random";

    String CONSISTENT_HASH = "consistentHash";

}
