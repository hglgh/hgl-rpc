package com.hgl.hglrpc.fault.retry;

import com.hgl.hglrpc.spi.SpiLoader;

/**
 * @ClassName: RetryStrategyFactory
 * @Package: com.hgl.hglrpc.fault.retry
 * @Description: 重试策略工厂（用于获取重试器对象）
 * @Author HGL
 * @Create: 2025/9/5 13:53
 */
public class RetryStrategyFactory {
    static {
        SpiLoader.load(RetryStrategy.class);
    }

    /**
     * 默认重试器
     */
    private static final RetryStrategy DEFAULT_RETRY_STRATEGY = new NoRetryStrategy();

    /**
     * 获取重试策略对象
     *
     * @param key 策略名称
     * @return RetryStrategy实例
     */
    public static RetryStrategy getInstance(String key) {
        return SpiLoader.getInstance(RetryStrategy.class, key);
    }
}
