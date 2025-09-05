package com.hgl.hglrpc.fault.retry;

import com.github.rholder.retry.*;
import com.hgl.hglrpc.model.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * @ClassName: FixedIntervalRetryStrategy
 * @Package: com.hgl.hglrpc.fault.retry
 * @Description: 固定时间间隔 - 重试策略
 * @Author HGL
 * @Create: 2025/9/5 11:20
 */
@Slf4j
public class FixedIntervalRetryStrategy implements RetryStrategy {
    @Override
    public RpcResponse doRetry(Callable<RpcResponse> callable) throws Exception {

        Retryer<RpcResponse> retryer = RetryerBuilder.<RpcResponse>newBuilder()
                .retryIfExceptionOfType(Exception.class)
                .withWaitStrategy(WaitStrategies.fixedWait(3L, TimeUnit.SECONDS))
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                .withRetryListener(new RetryListener() {
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {

                        long attemptNumber = attempt.getAttemptNumber();
                        if (attemptNumber > 1) {
                            log.info("重试次数 {}", attemptNumber - 1);
                        }
                    }
                })
                .build();
        return retryer.call(callable);
    }
}
