package com.hgl.hglrpc.fault.circuitbreaker;

/**
 * 熔断器状态枚举
 *
 * <pre>
 * 状态转换图：
 *
 *   失败次数 >= 阈值         等待超时到期
 * CLOSED ──────────▶ OPEN ──────────▶ HALF_OPEN
 *   ▲                                      │
 *   └──────────── 调用成功 ────────────────┘
 *                  │
 *                  └───── 调用失败 ────▶ OPEN
 * </pre>
 *
 * <ul>
 *   <li>CLOSED：正常状态，请求正常通过</li>
 *   <li>OPEN：熔断状态，直接拒绝请求（快速失败），不调用远程服务</li>
 *   <li>HALF_OPEN：半开状态，允许一次试探性调用，成功则恢复，失败则继续熔断</li>
 * </ul>
 *
 * @author HGL
 */
public enum CircuitBreakerState {

    /** 关闭（正常放行） */
    CLOSED,

    /** 打开（熔断中，直接拒绝） */
    OPEN,

    /** 半开（试探性放行一次） */
    HALF_OPEN
}
