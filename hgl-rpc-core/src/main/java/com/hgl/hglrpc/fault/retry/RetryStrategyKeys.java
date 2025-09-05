package com.hgl.hglrpc.fault.retry;

/**
 * @ClassName: RetryStrategyKeys
 * @Package: com.hgl.hglrpc.fault.retry
 * @Description: 重试策略键名常量
 * @Author HGL
 * @Create: 2025/9/5 13:20
 */
public interface RetryStrategyKeys {
    /**
     * 不重试
     */
    String NO = "no";

    /**
     * 固定时间间隔
     */
    String FIXED_INTERVAL = "fixedInterval";
}
