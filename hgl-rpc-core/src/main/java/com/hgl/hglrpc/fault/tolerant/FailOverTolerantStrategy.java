package com.hgl.hglrpc.fault.tolerant;

import cn.hutool.core.collection.CollUtil;
import com.hgl.hglrpc.RpcApplication;
import com.hgl.hglrpc.config.RpcConfig;
import com.hgl.hglrpc.fault.retry.RetryStrategy;
import com.hgl.hglrpc.fault.retry.RetryStrategyFactory;
import com.hgl.hglrpc.loadbalancer.LoadBalancer;
import com.hgl.hglrpc.loadbalancer.LoadBalancerFactory;
import com.hgl.hglrpc.model.RpcRequest;
import com.hgl.hglrpc.model.RpcResponse;
import com.hgl.hglrpc.model.ServiceMetaInfo;
import com.hgl.hglrpc.server.client.VertxClient;
import com.hgl.hglrpc.server.client.VertxClientFactory;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 故障转移容错策略 —— 「这家快递不行？换一家试试！」
 *
 * <p>当调用某个服务节点失败时，不急着报错，而是把请求转移到其他可用节点上重试。
 * 就像你叫了个外卖，骑手A送不了，系统自动给你换骑手B。</p>
 *
 * <p><b>核心思路：</b>集群环境下，同一个服务通常部署在多个节点上。
 * 一个节点挂了不代表所有节点都挂了，换一个还能用的节点试试。</p>
 *
 * <pre>
 * 故障转移流程：
 *
 *   ┌──────────┐        ┌──────────┐        ┌──────────┐
 *   │  节点 A   │        │  节点 B   │        │  节点 C   │
 *   │ (已故障)  │        │ (健康)    │        │ (健康)    │
 *   └─────┬────┘        └──────────┘        └──────────┘
 *         │
 *   请求 ─▶ 节点 A        失败！
 *         │                    │
 *         │         ┌──────────┘
 *         │         ▼
 *         │    从可用列表中移除节点 A
 *         │         │
 *         │         ▼
 *         │    ┌────────────────────────────┐
 *         │    │  负载均衡器（LoadBalancer）  │
 *         │    │  从剩余节点中选一个          │
 *         │    └─────────┬──────────────────┘
 *         │              │
 *         │              ▼
 *         │         请求 ─▶ 节点 B        成功！
 *         │                         │
 *         │                         ▼
 *         │                    返回结果给客户端
 *         │
 *         ▼
 *    如果所有节点都失败，才抛出异常
 * </pre>
 *
 * <p><b>实现细节：</b></p>
 * <ul>
 *   <li>使用服务节点列表的<strong>副本</strong>，避免修改调用方的共享列表</li>
 *   <li>每次转移前，通过负载均衡器重新选择节点（而不是简单地按顺序试）</li>
 *   <li>每个新节点也会配合重试策略进行调用</li>
 *   <li>所有节点都试过了还失败，才最终抛出异常</li>
 * </ul>
 *
 * @author HGL
 * @since 2025/9/5
 */
@Slf4j
public class FailOverTolerantStrategy implements TolerantStrategy {

    /**
     * 执行故障转移：尝试调用其他可用节点，直到成功或所有节点都失败。
     *
     * <p>这个方法就像一个尽职尽责的快递调度员：这家网点送不了，马上安排下一家，
     * 直到找到一个能送的网点，或者确认所有网点都不行了才放弃。</p>
     *
     * @param context 上下文信息，包含：
     *                <ul>
     *                  <li>"rpcRequest" — 原始 RPC 请求</li>
     *                  <li>"serviceMetaInfoList" — 所有可用服务节点列表</li>
     *                  <li>"selectedServiceMetaInfo" — 之前失败的节点</li>
     *                </ul>
     * @param e       原始异常（首次调用失败的原因）
     * @return RpcResponse 故障转移成功后的响应结果
     * @throws RuntimeException 所有可用节点都调用失败时抛出
     */
    @Override
    @SuppressWarnings("unchecked")
    public RpcResponse doTolerant(Map<String, Object> context, Exception e) {
        // ========== 第一步：从上下文中取出关键信息 ==========
        RpcRequest rpcRequest = (RpcRequest) context.get("rpcRequest");
        List<ServiceMetaInfo> serviceMetaInfoList = (List<ServiceMetaInfo>) context.get("serviceMetaInfoList");
        ServiceMetaInfo selectedServiceMetaInfo = (ServiceMetaInfo) context.get("selectedServiceMetaInfo");

        // ========== 第二步：构建可用节点列表（使用副本，保护原始数据）==========
        // 为什么要用副本？因为 serviceMetaInfoList 可能被多个线程共享，
        // 直接 remove 会导致并发问题，就像多人同时改一张表格会乱套
        List<ServiceMetaInfo> availableNodes = new ArrayList<>(serviceMetaInfoList);

        // 把之前失败的节点踢出候选名单（既然已经证明它不行了，就别再去烦它了）
        removeFailNode(selectedServiceMetaInfo, availableNodes);

        // ========== 第三步：准备负载均衡器和相关配置 ==========
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        LoadBalancer loadBalancer = LoadBalancerFactory.getInstance(rpcConfig.getLoadBalancer());
        Map<String, Object> requestParamMap = new HashMap<>();
        requestParamMap.put("methodName", rpcRequest.getMethodName());

        // ========== 第四步：逐个尝试剩余节点 ==========
        RpcResponse rpcResponse;
        while (!availableNodes.isEmpty()) {
            // 通过负载均衡器选择下一个节点（不是简单按顺序，而是考虑权重等因素）
            ServiceMetaInfo currentServiceMetaInfo = loadBalancer.select(requestParamMap, availableNodes);
            log.info("获取节点：{}", currentServiceMetaInfo);

            try {
                // 对当前节点发起调用（也带重试策略哦，双保险）
                RetryStrategy retryStrategy = RetryStrategyFactory.getInstance(rpcConfig.getRetryStrategy());
                VertxClient vertxClient = VertxClientFactory.getInstance(rpcConfig.getProtocol());
                rpcResponse = retryStrategy.doRetry(() -> {
                    try {
                        return vertxClient.doRequest(rpcRequest, currentServiceMetaInfo);
                    } catch (Throwable ex) {
                        throw new RuntimeException(ex);
                    }
                });
                // 成功了！赶紧返回结果
                return rpcResponse;
            } catch (Exception exception) {
                // 这个节点也不行，移除它，继续试下一个
                removeFailNode(currentServiceMetaInfo, availableNodes);
            }
        }

        // ========== 第五步：所有节点都试过了，全部失败 ==========
        // 就像所有快递网点都关门了，这包裹真的送不出去了
        throw new RuntimeException(e);
    }

    /**
     * 从可用节点列表中移除指定的失败节点。
     *
     * <p>通过比较节点的 ServiceNodeKey（节点唯一标识）来匹配和移除。
     * 就像从通讯录里拉黑一个号码。</p>
     *
     * @param currentServiceMetaInfo 要移除的失败节点
     * @param serviceMetaInfoList    可用节点列表（会被修改）
     */
    private void removeFailNode(ServiceMetaInfo currentServiceMetaInfo, List<ServiceMetaInfo> serviceMetaInfoList) {
        if (CollUtil.isNotEmpty(serviceMetaInfoList)) {
            serviceMetaInfoList.removeIf(next -> currentServiceMetaInfo.getServiceNodeKey().equals(next.getServiceNodeKey()));
        }
    }
}
