package com.hgl.hglrpc.fault.circuitbreaker;

import com.hgl.hglrpc.model.RpcResponse;

import java.util.concurrent.Callable;

/**
 * 熔断器接口 —— "电路保险丝"
 *
 * <p>当远程服务连续失败达到阈值时，熔断器自动打开（OPEN），
 * 后续请求直接快速失败，不再调用远程服务。这样可以：
 * <ul>
 *   <li>避免对已经不可用的服务持续发起无意义的请求（保护调用方资源）</li>
 *   <li>给故障服务恢复的时间（保护被调用方）</li>
 *   <li>防止级联故障扩散到整个系统</li>
 * </ul>
 *
 * <pre>
 * 工作流程：
 *
 *   RPC 调用
 *      │
 *      ▼
 *   ┌──────────────────────┐
 *   │   CircuitBreaker     │
 *   │   当前状态？          │
 *   └──────┬─────┬─────┬───┘
 *          │     │     │
 *     CLOSED   OPEN  HALF_OPEN
 *          │     │     │
 *          ▼     ▼     ▼
 *      放行    快速   试探一次
 *      调用   失败    调用
 *          │     │     │
 *          │     │     ├─ 成功 → CLOSED
 *          │     │     └─ 失败 → OPEN
 *          │     │
 *          ▼     │
 *    记录成功/失败│
 *    成功→重置计数│
 *    失败→累加计数│
 *    达到阈值→OPEN│
 * </pre>
 *
 * @author HGL
 * @see DefaultCircuitBreaker 默认实现
 */
public interface CircuitBreaker {

    /**
     * 执行带熔断保护的调用。
     *
     * @param callable 要执行的 RPC 调用
     * @return RpcResponse 调用成功的响应
     * @throws Exception 熔断打开时抛出 RuntimeException，或调用失败时抛出原始异常
     */
    RpcResponse execute(Callable<RpcResponse> callable) throws Exception;

    /**
     * 获取当前熔断器状态。
     */
    CircuitBreakerState getState();

    /**
     * 重置熔断器到关闭状态。
     */
    void reset();
}
