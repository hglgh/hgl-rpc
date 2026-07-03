package com.hgl.hglrpc.loadbalancer;

import com.hgl.hglrpc.model.ServiceMetaInfo;

import java.util.List;
import java.util.Map;

/**
 * LoadBalancer —— 负载均衡器的工作规范（接口）
 *
 * <h2>角色比喻：快递调度中心的"派件规则"</h2>
 * <p>
 * 想象一个快递站点有 N 个快递员（服务提供者）都在待命，
 * 每当一个新的包裹（RPC 请求）到来时，<b>调度员</b>需要决定把这个包裹交给哪位快递员去派送。
 * LoadBalancer 就是调度员必须遵守的"派件规则手册"。
 * </p>
 *
 * <h2>为什么需要负载均衡？</h2>
 * <pre>
 *   调用方（消费者）
 *       │
 *       ▼  RPC 请求
 *  ┌──────────────┐
 *  │  LoadBalancer │  ←── "选谁来干活？"
 *  └──────┬───────┘
 *         │
 *    ┌────┼────┬────────┐
 *    ▼    ▼    ▼        ▼
 *  [S1]  [S2]  [S3]   [S4]   ←── 多个服务提供者（快递员）
 * </pre>
 * <p>
 * 如果所有请求都砸向同一台服务器，它会被压垮，其他服务器却闲着。
 * 负载均衡的目的就是<b>把请求均匀（或按策略）分摊到多个服务节点上</b>，
 * 提高整体吞吐量，避免单点过载。
 * </p>
 *
 * <h2>不同策略 = 不同的选人哲学</h2>
 * <ul>
 *   <li><b>轮询（Round Robin）</b>—— 排队轮流来，公平但不考虑服务器负载</li>
 *   <li><b>随机（Random）</b>—— 掷骰子选人，简单粗暴</li>
 *   <li><b>一致性哈希（Consistent Hash）</b>—— 同样的请求尽量落到同一个节点，
 *       天然适合有缓存亲和性的场景</li>
 * </ul>
 * <p>
 * 本接口只定义"怎么选"的规范，具体策略由实现类决定，
 * 通过 SPI 机制可插拔替换，做到"换调度员不换站点"。
 * </p>
 *
 * @author HGL
 * @see RoundRobinLoadBalancer   轮询策略
 * @see RandomLoadBalancer       随机策略
 * @see ConsistentHashLoadBalancer  一致性哈希策略
 */
public interface LoadBalancer {

    /**
     * 从可用的服务列表中选择一个来执行调用——"选哪个快递员来送这个包裹？"
     *
     * <p>调度员需要两样信息来做决定：</p>
     * <ul>
     *   <li>{@code requestParams}  —— 本次请求的参数（快递单上的信息），
     *       一致性哈希等策略会用请求内容来决定路由</li>
     *   <li>{@code serviceMetaInfoList}  —— 当前所有可用的服务提供者列表（待命的快递员花名册），
     *       由注册中心返回</li>
     * </ul>
     *
     * @param requestParams       请求参数，不同策略可能依赖它做路由决策；
     *                            例如一致性哈希会根据请求参数计算哈希值
     * @param serviceMetaInfoList 可用服务节点列表（不为空，由调用方保证）
     * @return 被选中的服务节点元信息；如果列表为空则返回 {@code null}（没有快递员可派）
     */
    ServiceMetaInfo select(Map<String, Object> requestParams, List<ServiceMetaInfo> serviceMetaInfoList);
}
