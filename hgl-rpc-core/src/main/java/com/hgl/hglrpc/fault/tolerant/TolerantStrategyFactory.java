package com.hgl.hglrpc.fault.tolerant;

import com.hgl.hglrpc.spi.SpiLoader;

/**
 * @ClassName: TolerantStrategyFactory
 * @Package: com.hgl.hglrpc.fault.tolerant
 * @Description: 容错策略工厂（工厂模式，用于获取容错策略对象）
 * @Author HGL
 * @Create: 2025/9/5 14:37
 */
public class TolerantStrategyFactory {

    static {
        SpiLoader.load(TolerantStrategy.class);
    }

    /**
     * 默认容错策略
     */
    private static final TolerantStrategy DEFAULT_RETRY_STRATEGY = new FailFastTolerantStrategy();

    /**
     * 获取实例
     *
     * @param key 键
     * @return 实例
     */
    public static TolerantStrategy getInstance(String key) {
        return SpiLoader.getInstance(TolerantStrategy.class, key);
    }

}
