package com.hgl.hglrpc.loadbalancer;

import com.hgl.hglrpc.spi.SpiLoader;

/**
 * LoadBalancerFactory —— 调度员的"人事部"（负载均衡器工厂）
 *
 * <h2>职责：根据工号找到调度员</h2>
 * <p>
 * 当 RPC 框架需要做负载均衡时，不会自己 new 一个具体的调度员，
 * 而是到"人事部"（本工厂）说："给我一个 roundRobin 调度员"。
 * 人事部通过 SPI 机制查找并返回对应的实现。
 * </p>
 *
 * <h2>工厂 + SPI 的好处</h2>
 * <pre>
 *  ┌─────────────────────────────────────────────────────┐
 *  │                  调用方（业务代码）                    │
 *  │  LoadBalancer lb =                                  │
 *  │      LoadBalancerFactory.getInstance("roundRobin");  │
 *  └──────────────────────┬──────────────────────────────┘
 *                         │
 *                         ▼
 *  ┌─────────────────────────────────────────────────────┐
 *  │              LoadBalancerFactory（人事部）             │
 *  │  内部委托给 SPI 机制                                  │
 *  └──────────────────────┬──────────────────────────────┘
 *                         │
 *                         ▼
 *  ┌─────────────────────────────────────────────────────┐
 *  │              SpiLoader（插件管理器）                   │
 *  │  读取配置文件 / SPI 声明，找到 key 对应的实现类          │
 *  │  "roundRobin" → RoundRobinLoadBalancer.class        │
 *  │  "random"     → RandomLoadBalancer.class            │
 *  │  "consistentHash" → ConsistentHashLoadBalancer.class│
 *  └──────────────────────┬──────────────────────────────┘
 *                         │
 *                         ▼
 *  返回具体的 LoadBalancer 实例（懒加载 + 单例缓存）
 * </pre>
 *
 * <h2>为什么要用工厂模式？</h2>
 * <ul>
 *   <li><b>解耦</b>：调用方不需要知道具体实现类的名字，只需传入一个 key</li>
 *   <li><b>可扩展</b>：新增负载均衡策略时，只需实现接口 + 配置 SPI，
 *       不需要修改工厂代码，符合开闭原则（OCP）</li>
 *   <li><b>统一管理</b>：所有负载均衡器的创建和获取都经过同一个入口，
 *       方便做缓存、日志、监控等横切关注点</li>
 * </ul>
 *
 * @author HGL
 * @see LoadBalancer
 * @see LoadBalancerKeys
 * @see SpiLoader
 */
public class LoadBalancerFactory {

    /**
     * 根据 key 获取负载均衡器实例——"按工号在人事系统里查人"
     * <p>
     * 内部委托给 {@link SpiLoader}，SPI 机制会：
     * <ol>
     *   <li>查找配置文件中 key 对应的实现类</li>
     *   <li>反射创建实例（首次）或返回缓存的单例（后续）</li>
     * </ol>
     * </p>
     *
     * @param key 负载均衡策略的标识，取值参见 {@link LoadBalancerKeys}：
     *            "roundRobin"、"random"、"consistentHash"
     * @return 对应的 LoadBalancer 实例
     */
    public static LoadBalancer getInstance(String key) {
        return SpiLoader.getInstance(LoadBalancer.class, key);
    }
}
