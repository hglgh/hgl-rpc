package com.hgl.hglrpc.fault.retry;

import com.hgl.hglrpc.model.RpcResponse;

import java.util.concurrent.Callable;

/**
 * 不重试策略 —— 「失败了就失败了，绝不回头」
 *
 * <p>这是最简单、最「硬气」的重试策略：只调用一次，成功就成功，失败就直接抛异常。
 * 就像寄快递用平邮：寄出去就不管了，丢了就是丢了，绝不补寄。</p>
 *
 * <p><b>适用场景：</b></p>
 * <ul>
 *   <li>幂等性不确定的操作（比如扣款，重试可能多扣钱）</li>
 *   <li>对实时性要求极高，不允许等待重试</li>
 *   <li>错误是确定性的（比如参数错误），重试也不会成功</li>
 * </ul>
 *
 * <pre>
 * 调用流程：
 *
 *   客户端 ──请求──▶ 服务端
 *                   │
 *          ┌────────┴────────┐
 *          ▼                 ▼
 *       [成功]            [失败]
 *          │                 │
 *       返回结果          直接抛异常（不重试！）
 *
 *   重试次数: 0 次
 *   等待时间: 无
 * </pre>
 *
 * @author HGL
 * @since 2025/9/5
 */
public class NoRetryStrategy implements RetryStrategy {

    /**
     * 直接执行调用，不做任何重试。
     *
     * <p>方法名虽然叫 doRetry，但这里的实现是：试都不试，直接调一次就完事。
     * 「叫这个名字只是因为要遵守接口规范，但我内心毫无重试的打算。」</p>
     *
     * @param callable 要执行的 RPC 调用
     * @return RpcResponse 调用结果
     * @throws Exception 调用失败时直接抛出，不做任何补救
     */
    @Override
    public RpcResponse doRetry(Callable<RpcResponse> callable) throws Exception {
        // 干脆利落：调一次，成不成看天意
        return callable.call();
    }
}
