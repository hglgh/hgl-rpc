package com.hgl.hglrpc.loadbalancer;

/**
 * LoadBalancerKeys —— 调度员的"工号牌"（负载均衡策略常量）
 *
 * <h2>作用</h2>
 * <p>
 * 就像每个快递调度员上岗时都有一个<b>工号</b>，
 * 配置文件里写 "roundRobin" 就能找到轮询调度员，
 * 写 "random" 就能找到随机调度员。
 * 这些常量就是调度员的工号，用于：
 * </p>
 * <ul>
 *   <li><b>SPI 配置文件</b>：在 META-INF/services/ 或配置文件中声明映射关系</li>
 *   <li><b>框架配置</b>：用户在 RpcConfig 中指定想用哪种负载均衡策略</li>
 *   <li><b>工厂查找</b>：{@link LoadBalancerFactory} 根据 key 获取对应的实现类实例</li>
 * </ul>
 *
 * <h2>查找流程</h2>
 * <pre>
 *  用户配置: loadBalancer = "roundRobin"
 *       │
 *       ▼
 *  LoadBalancerFactory.getInstance("roundRobin")
 *       │
 *       ▼
 *  SPI 机制查找: key → 实现类
 *       │
 *       ▼
 *  返回 RoundRobinLoadBalancer 实例
 * </pre>
 *
 * @author HGL
 * @see LoadBalancerFactory
 * @see LoadBalancer
 */
public interface LoadBalancerKeys {

    /**
     * 轮询策略的工号——"roundRobin"
     * <p>
     * 对应 {@link RoundRobinLoadBalancer}，
     * 按顺序轮流分配请求，保证每个节点获得大致相等的请求数。
     * </p>
     */
    String ROUND_ROBIN = "roundRobin";

    /**
     * 随机策略的工号——"random"
     * <p>
     * 对应 {@link RandomLoadBalancer}，
     * 随机选择一个节点，利用大数定律实现统计意义上的均匀分布。
     * </p>
     */
    String RANDOM = "random";

    /**
     * 一致性哈希策略的工号——"consistentHash"
     * <p>
     * 对应 {@link ConsistentHashLoadBalancer}，
     * 同样的请求参数总是路由到同一个节点，适合有缓存亲和性的场景。
     * </p>
     */
    String CONSISTENT_HASH = "consistentHash";
}
