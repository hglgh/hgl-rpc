package com.hgl.hglrpc.fault.retry;

import com.hgl.hglrpc.model.RpcResponse;

import java.util.concurrent.Callable;

/**
 * 重试策略的工作规范（接口）
 *
 * <p>想象一下快递场景：你寄一个包裹出去，对方没收到怎么办？
 * 是再寄一次？还是换一家快递公司？还是干脆算了？
 * 这个接口就是定义「失败后要不要再试一次」的行为规范。</p>
 *
 * <pre>
 * ┌─────────────────────────────────────────────────────┐
 * │                  RPC 调用流程                         │
 * │                                                      │
 * │   客户端  ──请求──▶  服务端                           │
 * │     │                   │                            │
 * │     │         ┌─────────┘                            │
 * │     │         ▼                                      │
 * │     │     [失败了!]                                   │
 * │     │         │                                      │
 * │     │         ▼                                      │
 * │   ┌───────────────────────┐                          │
 * │   │   RetryStrategy       │  ◀── 就是这个接口        │
 * │   │   "要不要再试？"       │                          │
 * │   └───────┬───────────────┘                          │
 * │           │                                          │
 * │     ┌─────┴──────┐                                   │
 * │     ▼            ▼                                   │
 * │  不重试        重试（间隔/退避等）                     │
 * └─────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>不同的实现类代表不同的重试策略：
 * <ul>
 *   <li>{@link NoRetryStrategy} —— 不重试，失败就失败了（像寄平邮丢了就算了）</li>
 *   <li>{@link FixedIntervalRetryStrategy} —— 每隔固定时间重试（像每隔3分钟去信箱看一次）</li>
 * </ul>
 * </p>
 *
 * @author HGL
 * @since 2025/9/5
 */
public interface RetryStrategy {

    /**
     * 执行带重试逻辑的调用。
     *
     * <p>这个方法会根据具体的重试策略，决定在失败时是否重试 callable。
     * 就像快递员送了一次没送到，根据公司规定决定是原地等、明天再来、还是直接退回。</p>
     *
     * @param callable 要执行的 RPC 调用（就像要送出去的「包裹」）
     * @return RpcResponse 调用成功时的响应结果
     * @throws Exception 如果所有重试都失败了，最终会抛出异常（包裹彻底送不到）
     */
    RpcResponse doRetry(Callable<RpcResponse> callable) throws Exception;
}
