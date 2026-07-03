package com.hgl.hglrpc.fault.retry;

/**
 * 重试策略的编号（SPI 键名常量）
 *
 * <p>每种重试策略都有一个「身份证号码」，这个接口里定义的就是这些号码。
 * 通过 SPI 机制（Service Provider Interface），我们可以根据这些 key
 * 动态加载对应的策略实现类，就像根据快递单号查到对应的快递公司一样。</p>
 *
 * <p><b>为什么需要这些常量？</b></p>
 * <p>想象一下，如果没有统一的命名，A 代码里写 "no"，B 代码里写 "none"，
 * C 代码里写 "no_retry"，最后谁也找不到谁。所以要有一个「花名册」
 * 统一管理这些策略名称。</p>
 *
 * <pre>
 * 策略选择流程：
 *
 *   配置文件/用户设置
 *         │
 *         ▼
 *   ┌──────────────┐
 *   │ key = "no"    │──▶  NoRetryStrategy（不重试）
 *   │ key = "fixed" │──▶  FixedIntervalRetryStrategy（固定间隔重试）
 *   └──────────────┘
 *         │
 *         ▼
 *   RetryStrategyFactory.getInstance(key)
 *   根据 key 通过 SPI 加载对应的实现类
 * </pre>
 *
 * @author HGL
 * @since 2025/9/5
 * @see RetryStrategyFactory 重试策略工厂，使用这些 key 来获取实例
 */
public interface RetryStrategyKeys {

    /**
     * 不重试策略的键名。
     *
     * <p>对应 {@link NoRetryStrategy}，调用失败直接抛异常，绝不姑息。
     * 就像选择「平邮」—— 丢了就丢了。</p>
     */
    String NO = "no";

    /**
     * 固定时间间隔重试策略的键名。
     *
     * <p>对应 {@link FixedIntervalRetryStrategy}，失败后每隔固定时间重试。
     * 就像选择「挂号快递」—— 送不到会再来一次。</p>
     */
    String FIXED_INTERVAL = "fixedInterval";
}
