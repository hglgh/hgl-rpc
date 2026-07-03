package com.hgl.hglrpc.loadbalancer;

import com.hgl.hglrpc.model.ServiceMetaInfo;

import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * RandomLoadBalancer —— 随机负载均衡器（"随机抽签的调度员"）
 *
 * <h2>核心思想：掷骰子选人</h2>
 * <p>
 * 快递站点来了一个包裹，调度员不看顺序、不看工龄，
 * 直接从花名册里<b>随机抽一个</b>快递员来派送。
 * 听起来"不靠谱"？其实当请求数量足够大时，
 * 大数定律保证每个人被抽到的概率趋近于 1/N，统计上是均匀的。
 * </p>
 *
 * <h2>工作流程图</h2>
 * <pre>
 *   请求到达
 *     │
 *     ▼
 *  random.nextInt(N)    ←── 生成 [0, N) 的随机数
 *     │
 *     ▼
 *  选中 serviceList[randomIndex]
 *
 *  概率分布示意（3 个服务节点）：
 *  ┌────────────────────────────────┐
 *  │  S0: ████████  33.3%          │
 *  │  S1: ████████  33.3%          │
 *  │  S2: ████████  33.3%          │
 *  └────────────────────────────────┘
 *  （样本量足够大时趋近均匀）
 * </pre>
 *
 * <h2>适用场景</h2>
 * <ul>
 *   <li><b>简单粗暴</b>：实现最简单，不需要维护状态</li>
 *   <li><b>短连接场景</b>：不需要考虑请求亲和性</li>
 *   <li><b>服务节点性能相近</b>：如果节点性能差异大，轮询或加权策略更合适</li>
 * </ul>
 *
 * <h2>与轮询的区别</h2>
 * <ul>
 *   <li>轮询严格保证 1:1:1 的分配比例，但可能在短时间内把连续请求全打到同一节点</li>
 *   <li>随机策略虽然不能保证精确均匀，但天然"打散"请求，避免了连续碰撞</li>
 *   <li>随机策略无需维护计数器，<b>无状态</b>，更适合短生命周期的调用</li>
 * </ul>
 *
 * @author HGL
 * @see LoadBalancer
 * @see LoadBalancerKeys#RANDOM
 */
public class RandomLoadBalancer implements LoadBalancer {

    /**
     * 随机数生成器——"调度员手里的骰子"
     * <p>
     * 注意：{@code java.util.Random} 不是线程安全的，
     * 但在高并发下多线程同时调用 {@code nextInt()} 时，
     * 内部的 CAS 机制会自动处理竞争，不会抛异常，
     * 只是可能有轻微的性能损失。如果对性能极其敏感，
     * 可以改用 {@code ThreadLocalRandom}（每个线程一个实例，完全无锁）。
     * </p>
     */
    private final Random random = new Random();

    /**
     * 随机选择一个服务节点——"从花名册里随便抽一个"
     *
     * @param requestParams       请求参数（随机策略不使用此参数）
     * @param serviceMetaInfoList 可用服务列表
     * @return 被选中的服务节点；列表为空时返回 {@code null}
     */
    @Override
    public ServiceMetaInfo select(Map<String, Object> requestParams, List<ServiceMetaInfo> serviceMetaInfoList) {
        int size = serviceMetaInfoList.size();
        // 没有快递员可派
        if (size == 0) {
            return null;
        }
        // 只有一个服务节点，不用随机——"只有一个快递员，直接派给他"
        if (size == 1) {
            return serviceMetaInfoList.get(0);
        }
        // nextInt(size) 返回 [0, size) 的随机整数，直接作为索引选取节点
        return serviceMetaInfoList.get(random.nextInt(size));
    }
}
