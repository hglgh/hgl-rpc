package com.hgl.hglrpc.fault.tolerant;

import com.hgl.hglrpc.spi.SpiLoader;

/**
 * 容错策略工厂 —— 「容错方案自助选择器」
 *
 * <p>根据传入的策略名称（key），通过 SPI 机制动态加载并返回对应的容错策略实例。
 * 就像去餐厅点菜：你说「来份 FailOver」，厨师（SpiLoader）就给你做一份故障转移策略。</p>
 *
 * <p><b>底层机制：</b>使用 SPI（Service Provider Interface）加载器，
 * 好处和 {@link com.hgl.hglrpc.fault.retry.RetryStrategyFactory} 一样：
 * 解耦、可扩展、统一管理。</p>
 *
 * <pre>
 * 工厂在整个容错体系中的角色：
 *
 *   ┌─────────────────────────────────────────────────┐
 *   │                容错体系总览                       │
 *   │                                                  │
 *   │   ┌──────────────────────┐                       │
 *   │   │   TolerantStrategy   │  ◀── 接口（规范）      │
 *   │   └──────────────────────┘                       │
 *   │          ▲  ▲  ▲  ▲                              │
 *   │          │  │  │  │                              │
 *   │   ┌─────┘  │  │  └──────┐                       │
 *   │   │        │  │         │                       │
 *   │  FailFast FailOver FailSafe FailBack            │
 *   │   (实现类)                                       │
 *   │                                                  │
 *   │   ┌──────────────────────┐                       │
 *   │   │ TolerantStrategyFactory│ ◀── 工厂（创建者）  │
 *   │   └──────────┬───────────┘                       │
 *   │              │                                   │
 *   │              ▼                                   │
 *   │   ┌──────────────────────┐                       │
 *   │   │     SpiLoader        │ ◀── SPI 加载器       │
 *   │   │  (动态发现和加载)     │                       │
 *   │   └──────────────────────┘                       │
 *   └─────────────────────────────────────────────────┘
 * </pre>
 *
 * @author HGL
 * @since 2025/9/5
 */
public class TolerantStrategyFactory {

    /**
     * 根据策略名称获取容错策略实例。
     *
     * <p>就像在自动售货机上按一下按钮，对应的商品就出来了。</p>
     *
     * @param key 策略名称，参见 {@link TolerantStrategyKeys} 中定义的常量
     *            （如 "failFast"、"failOver"、"failSafe"、"failBack"）
     * @return TolerantStrategy 对应的容错策略实例
     */
    public static TolerantStrategy getInstance(String key) {
        return SpiLoader.getInstance(TolerantStrategy.class, key);
    }
}
