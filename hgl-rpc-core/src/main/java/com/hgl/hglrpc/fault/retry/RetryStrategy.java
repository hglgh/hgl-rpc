package com.hgl.hglrpc.fault.retry;

import com.hgl.hglrpc.model.RpcResponse;

import java.util.concurrent.Callable;

/**
 * @ClassName: RetryStrategy
 * @Package: com.hgl.hglrpc.fault.retry
 * @Description: 重试策略
 * @Author HGL
 * @Create: 2025/9/5 11:17
 */
public interface RetryStrategy {

    /**
     * 重试
     *
     * @param callable 调用函数
     * @return RpcResponse
     * @throws Exception 抛出异常
     */
    RpcResponse doRetry(Callable<RpcResponse> callable) throws Exception;
}
