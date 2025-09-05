package com.hgl.hglrpc.fault.retry;

import com.hgl.hglrpc.model.RpcResponse;

import java.util.concurrent.Callable;

/**
 * @ClassName: NoRetryStrategy
 * @Package: com.hgl.hglrpc.fault.retry
 * @Description: 不重试 - 重试策略
 * @Author HGL
 * @Create: 2025/9/5 11:20
 */
public class NoRetryStrategy implements RetryStrategy{
    @Override
    public RpcResponse doRetry(Callable<RpcResponse> callable) throws Exception {
        return callable.call();
    }
}
