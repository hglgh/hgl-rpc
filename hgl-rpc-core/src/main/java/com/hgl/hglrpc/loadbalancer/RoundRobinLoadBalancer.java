package com.hgl.hglrpc.loadbalancer;

import com.hgl.hglrpc.model.ServiceMetaInfo;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * RoundRobinLoadBalancer —— 轮询负载均衡器（"轮流派件的调度员"）
 *
 * <h2>核心思想：排队轮流，雨露均沾</h2>
 * <p>
 * 就像快递站里的调度员，手里拿着一份快递员名单，
 * 每来一个包裹就按顺序指派：<b>第 1 个给张三，第 2 个给李四，第 3 个给王五，第 4 个又回到张三……</b>
 * 周而复始，确保每个快递员接到的包裹数量大致相同。
 * </p>
 *
 * <h2>工作流程图</h2>
 * <pre>
 *   请求到达
 *     │
 *     ▼
 *  counter.getAndIncrement()  ←── 原子操作，线程安全
 *     │
 *     ▼
 *  index = |count| % N        ←── 取模映射到 [0, N-1]
 *     │
 *     ▼
 *  选中 serviceList[index]
 *
 *  时间线示意（3 个服务节点）：
 *  ──────────────────────────────────────────
 *  请求 #1  请求 #2  请求 #3  请求 #4  请求 #5
 *    │        │        │        │        │
 *    ▼        ▼        ▼        ▼        ▼
 *  [S0]     [S1]     [S2]     [S0]     [S1]   ← 严格轮转
 *  ──────────────────────────────────────────
 * </pre>
 *
 * <h2>为什么用 ConcurrentHashMap？</h2>
 * <p>
 * RPC 框架是多线程环境，多个线程可能同时请求同一个服务。
 * 如果用普通的 {@code HashMap} + 非原子计数器，会出现：
 * <ul>
 *   <li>线程 A 读到 count=5，线程 B 也读到 count=5 → 两个请求选中同一个节点</li>
 *   <li>并发写 HashMap 可能导致死循环（Java 7 之前）或数据丢失</li>
 * </ul>
 * {@code ConcurrentHashMap} + {@code AtomicInteger} 确保线程安全且无锁，
 * 性能优于 {@code synchronized}。
 * </p>
 *
 * <h2>为什么每个服务用独立计数器？</h2>
 * <p>
 * 不同服务（如 UserService、OrderService）的提供者列表可能完全不同，
 * 如果共用一个全局计数器，对 UserService 的调用会干扰 OrderService 的轮询顺序。
 * 用 {@code serviceKey → counter} 的映射，让每个服务各自维护独立的计数。
 * </p>
 *
 * @author HGL
 * @see LoadBalancer
 * @see LoadBalancerKeys#ROUND_ROBIN
 */
public class RoundRobinLoadBalancer implements LoadBalancer {

    /**
     * 每个服务独立的轮询计数器——"每个快递站点有自己的派件序号本"
     * <p>
     * key: 服务键名（如 "com.hgl.UserService:1.0"），标识一个具体的服务
     * value: 该服务的轮询索引（从 0 开始，每次调用自增 1）
     * </p>
     * <p>
     * 使用 ConcurrentHashMap 而非 Collections.synchronizedMap，
     * 因为前者采用分段锁/CAS，在高并发下吞吐量更高。
     * </p>
     */
    private final Map<String, AtomicInteger> serviceCounterMap = new ConcurrentHashMap<>();

    /**
     * 按轮询策略选择一个服务节点。
     *
     * @param requestParams       请求参数（轮询策略不使用此参数，但接口契约要求传入）
     * @param serviceMetaInfoList 可用服务列表（快递员花名册）
     * @return 被选中的服务节点；列表为空时返回 {@code null}
     */
    @Override
    public ServiceMetaInfo select(Map<String, Object> requestParams, List<ServiceMetaInfo> serviceMetaInfoList) {
        // 没有快递员可派，直接返回空
        if (serviceMetaInfoList.isEmpty()) {
            return null;
        }

        // 只有一个服务节点，无需轮询——"只有一个快递员，直接派给他"
        if (serviceMetaInfoList.size() == 1) {
            return serviceMetaInfoList.get(0);
        }

        // 用第一个服务的键名作为该组服务的标识
        // 例如 "com.hgl.UserService:1.0"
        String serviceKey = serviceMetaInfoList.get(0).getServiceKey();

        // 获取或创建该服务的轮询计数器
        // computeIfAbsent 是原子操作，保证并发安全，避免重复创建
        AtomicInteger counter = serviceCounterMap.computeIfAbsent(
                serviceKey,
                k -> new AtomicInteger(0)   // 首次访问时初始化为 0
        );

        // 原子递增并取模：
        //   1. getAndIncrement() 原子地返回旧值并 +1，保证每个线程拿到不同的序号
        //   2. Math.abs() 防止 int 溢出变为负数
        //   3. % size 映射到 [0, size-1] 的有效索引范围
        // 这样就实现了"轮流坐庄"的效果
        int index = Math.abs(counter.getAndIncrement()) % serviceMetaInfoList.size();
        return serviceMetaInfoList.get(index);
    }
}
