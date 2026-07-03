package com.hgl.hglrpc.fault.tolerant;

import com.hgl.hglrpc.model.RpcResponse;

import java.util.Map;

/**
 * 容错策略的工作规范（接口）
 *
 * <p>如果说 {@link com.hgl.hglrpc.fault.retry.RetryStrategy} 解决的是
 * 「失败了要不要再试一次」的问题，那么这个接口解决的是
 * 「再试了还是不行，怎么办」的问题。</p>
 *
 * <p>继续用快递的比喻：
 * 重试策略是「送不到的话要不要再送一次」，
 * 容错策略是「再送了还是送不到，要怎么善后」——
 * 是直接告诉客户送不了？换个快递公司送？还是默默签收当没事发生？</p>
 *
 * <pre>
 * 容错策略在整个 RPC 调用链中的位置：
 *
 *   客户端
 *     │
 *     ▼
 *   ┌─────────────────────────────────────┐
 *   │          1. 正常 RPC 调用            │
 *   │             失败了？                  │
 *   └──────────────┬──────────────────────┘
 *                  ▼
 *   ┌─────────────────────────────────────┐
 *   │          2. 重试策略（RetryStrategy） │
 *   │         重试了还是不行？              │
 *   └──────────────┬──────────────────────┘
 *                  ▼
 *   ┌─────────────────────────────────────┐  ◀── 就是这个接口
 *   │        3. 容错策略（TolerantStrategy）│
 *   │           怎么善后？                  │
 *   └──────────────┬──────────────────────┘
 *                  │
 *       ┌──────┬───┴────┬──────────┐
 *       ▼      ▼        ▼          ▼
 *    快速失败  故障转移  安全失败   失败回调
 *   (FailFast)(FailOver)(FailSafe)(FailBack)
 * </pre>
 *
 * <p>每种容错策略的选择取决于业务场景：
 * <ul>
 *   <li>支付系统 → FailFast（钱的事不能含糊）</li>
 *   <li>搜索查询 → FailSafe（搜不到返回空，页面别崩就行）</li>
 *   <li>集群服务 → FailOver（这台不行换那台）</li>
 *   <li>日志/监控 → FailBack（暂时记录，后面再补发）</li>
 * </ul>
 * </p>
 *
 * @author HGL
 * @since 2025/9/5
 */
public interface TolerantStrategy {

    /**
     * 执行容错处理。
     *
     * <p>当 RPC 调用经过重试后仍然失败时，调用此方法进行善后处理。
     * 不同的实现类有不同的善后方式，就像不同的快递公司有不同的理赔规则。</p>
     *
     * @param context 上下文信息，用于传递调用所需的数据。
     *                通常包含：
     *                <ul>
     *                  <li>"rpcRequest" — 原始的 RPC 请求（要送的包裹信息）</li>
     *                  <li>"serviceMetaInfoList" — 可用服务节点列表（可用的快递网点）</li>
     *                  <li>"selectedServiceMetaInfo" — 之前选中的节点（之前选的快递网点）</li>
     *                </ul>
     * @param e       捕获到的异常（「包裹送失败的原因」）
     * @return RpcResponse 容错处理后的响应结果
     */
    RpcResponse doTolerant(Map<String, Object> context, Exception e);
}
