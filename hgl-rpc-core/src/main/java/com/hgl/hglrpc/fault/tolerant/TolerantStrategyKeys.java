package com.hgl.hglrpc.fault.tolerant;

/**
 * @ClassName: TolerantStrategyKeys
 * @Package: com.hgl.hglrpc.fault.tolerant
 * @Description: 容错策略键名常量
 * @Author HGL
 * @Create: 2025/9/5 14:36
 */
public interface TolerantStrategyKeys {
    /**
     * 故障恢复
     */
    String FAIL_BACK = "failBack";

    /**
     * 快速失败
     */
    String FAIL_FAST = "failFast";

    /**
     * 故障转移
     */
    String FAIL_OVER = "failOver";

    /**
     * 静默处理
     */
    String FAIL_SAFE = "failSafe";
}
