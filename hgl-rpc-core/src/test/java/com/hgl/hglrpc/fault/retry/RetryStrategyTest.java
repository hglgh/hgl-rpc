package com.hgl.hglrpc.fault.retry;

import com.hgl.hglrpc.model.RpcResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @ClassName: RetryStrategyTest
 * @Package: com.hgl.hglrpc.fault.retry
 * @Description: 重试策略测试
 * @Author HGL
 * @Create: 2025/9/5 11:29
 */
@Slf4j
class RetryStrategyTest {
    RetryStrategy retryStrategy = new FixedIntervalRetryStrategy();

    @Test
    public void doRetry() {
        try {
            RpcResponse rpcResponse = retryStrategy.doRetry(() -> {
                System.out.println("测试重试");
                throw new RuntimeException("模拟重试失败");
            });
            System.out.println(rpcResponse);
        } catch (Exception e) {
            System.out.println("重试多次失败");
            log.error("Error: ", e);
        }
    }
}