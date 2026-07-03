package com.hgl.hglrpc.fault.retry;

import com.hgl.hglrpc.spi.SpiLoader;

/**
 * 重试策略工厂 —— 「重试策略自动售货机」
 *
 * <p>这个工厂类负责根据策略名称（key）创建对应的重试策略实例。
 * 你告诉它 "我要 fixedInterval"，它就给你一个 {@link FixedIntervalRetryStrategy}；
 * 你说 "我要 no"，它就给你一个 {@link NoRetryStrategy}。</p>
 *
 * <p><b>底层机制：</b>使用 SPI（Service Provider Interface）加载器来动态发现和
 * 加载策略实现类。这样做的好处是：</p>
 * <ul>
 *   <li><b>解耦</b>：调用方不需要知道具体实现类的名字</li>
 *   <li><b>可扩展</b>：新增策略只需添加实现类和 SPI 配置，不用改工厂代码</li>
 *   <li><b>统一管理</b>：所有策略实例的获取都走这个工厂</li>
 * </ul>
 *
 * <pre>
 * 使用流程：
 *
 *   调用方代码
 *       │
 *       │  "给我一个 fixedInterval 的重试策略"
 *       ▼
 *   RetryStrategyFactory.getInstance("fixedInterval")
 *       │
 *       ▼
 *   SpiLoader 从 SPI 配置中查找 key="fixedInterval" 对应的类
 *       │
 *       ▼
 *   返回 FixedIntervalRetryStrategy 实例
 * </pre>
 *
 * @author HGL
 * @since 2025/9/5
 */
public class RetryStrategyFactory {

    /**
     * 根据策略名称获取重试策略实例。
     *
     * <p>就像自动售货机：你投币（传入 key），它吐出商品（返回实例）。</p>
     *
     * @param key 策略名称，参见 {@link RetryStrategyKeys} 中定义的常量
     *            （如 "no"、"fixedInterval"）
     * @return RetryStrategy 对应的重试策略实例
     */
    public static RetryStrategy getInstance(String key) {
        return SpiLoader.getInstance(RetryStrategy.class, key);
    }
}
