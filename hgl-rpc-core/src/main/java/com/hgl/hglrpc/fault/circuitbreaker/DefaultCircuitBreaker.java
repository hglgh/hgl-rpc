package com.hgl.hglrpc.fault.circuitbreaker;

import com.hgl.hglrpc.model.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 默认熔断器实现 —— "可自动恢复的电路保险丝"
 *
 * <p>基于状态机模式实现熔断逻辑：
 * <pre>
 *   CLOSED（正常）                OPEN（熔断）               HALF_OPEN（半开）
 *   ┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
 *   │ 失败计数 < 阈值   │     │ 直接拒绝请求     │     │ 允许一次试探调用  │
 *   │ 放行所有请求      │ ──▶ │ 等待超时到期     │ ──▶ │ 成功 → CLOSED   │
 *   │ 达到阈值 → OPEN  │     │ 超时到 → HALF_OPEN│     │ 失败 → OPEN     │
 *   └─────────────────┘     └─────────────────┘     └─────────────────┘
 * </pre>
 *
 * <p>线程安全：使用 {@link AtomicReference} 管理状态，{@link AtomicInteger} 管理计数器，
 * volatile + synchronized 保证状态转换的原子性。
 *
 * @author HGL
 */
@Slf4j
public class DefaultCircuitBreaker implements CircuitBreaker {

    /** 当前状态 */
    private final AtomicReference<CircuitBreakerState> state = new AtomicReference<>(CircuitBreakerState.CLOSED);

    /** 连续失败次数计数器 */
    private final AtomicInteger failureCount = new AtomicInteger(0);

    /** 连续成功次数计数器（半开状态恢复用） */
    private final AtomicInteger successCount = new AtomicInteger(0);

    /** 进入 OPEN 状态的时间戳（用于判断超时） */
    private volatile long openTimestamp;

    /** 触发熔断的连续失败次数阈值（默认 5 次） */
    private final int failureThreshold;

    /** OPEN → HALF_OPEN 的等待时间（毫秒，默认 30 秒） */
    private final long openTimeoutMs;

    /** HALF_OPEN → CLOSED 需要的连续成功次数（默认 2 次） */
    private final int halfOpenSuccessThreshold;

    /**
     * 使用默认参数构造：失败阈值=5，超时=30秒，半开恢复需成功=2次
     */
    public DefaultCircuitBreaker() {
        this(5, 30_000L, 2);
    }

    /**
     * 自定义参数构造。
     *
     * @param failureThreshold        连续失败多少次后触发熔断
     * @param openTimeoutMs           熔断持续时间（毫秒）
     * @param halfOpenSuccessThreshold 半开状态恢复需要的连续成功次数
     */
    public DefaultCircuitBreaker(int failureThreshold, long openTimeoutMs, int halfOpenSuccessThreshold) {
        this.failureThreshold = failureThreshold;
        this.openTimeoutMs = openTimeoutMs;
        this.halfOpenSuccessThreshold = halfOpenSuccessThreshold;
    }

    @Override
    public RpcResponse execute(Callable<RpcResponse> callable) throws Exception {
        CircuitBreakerState currentState = state.get();

        // ========== OPEN 状态：检查是否超时 ==========
        if (currentState == CircuitBreakerState.OPEN) {
            if (System.currentTimeMillis() - openTimestamp >= openTimeoutMs) {
                // 超时了，转为半开状态，允许一次试探
                transitionTo(CircuitBreakerState.HALF_OPEN);
                log.info("熔断器超时到期，转为 HALF_OPEN");
            } else {
                // 还在熔断中，快速失败
                throw new RuntimeException("熔断器已打开（OPEN），拒绝调用。请等待超时后重试。");
            }
        }

        // ========== CLOSED 或 HALF_OPEN：放行调用 ==========
        try {
            RpcResponse response = callable.call();
            onSuccess();
            return response;
        } catch (Exception e) {
            onFailure();
            throw e;
        }
    }

    /**
     * 调用成功时的处理逻辑。
     *
     * <p>CLOSED 状态：重置失败计数
     * <p>HALF_OPEN 状态：累加成功计数，达到阈值后恢复为 CLOSED
     */
    private void onSuccess() {
        CircuitBreakerState currentState = state.get();
        if (currentState == CircuitBreakerState.CLOSED) {
            // 正常状态，成功则重置失败计数
            failureCount.set(0);
        } else if (currentState == CircuitBreakerState.HALF_OPEN) {
            // 半开状态，累加成功计数
            int successes = successCount.incrementAndGet();
            if (successes >= halfOpenSuccessThreshold) {
                // 连续成功达到阈值，恢复为正常
                transitionTo(CircuitBreakerState.CLOSED);
                log.info("半开状态连续 {} 次成功，熔断器恢复为 CLOSED", successes);
            }
        }
    }

    /**
     * 调用失败时的处理逻辑。
     *
     * <p>CLOSED 状态：累加失败计数，达到阈值后触发熔断
     * <p>HALF_OPEN 状态：立即重新熔断
     */
    private void onFailure() {
        CircuitBreakerState currentState = state.get();
        if (currentState == CircuitBreakerState.CLOSED) {
            int failures = failureCount.incrementAndGet();
            if (failures >= failureThreshold) {
                // 连续失败达到阈值，触发熔断
                transitionTo(CircuitBreakerState.OPEN);
                log.warn("连续失败 {} 次达到阈值，熔断器打开（OPEN）", failures);
            }
        } else if (currentState == CircuitBreakerState.HALF_OPEN) {
            // 半开状态下失败，立刻重新熔断
            transitionTo(CircuitBreakerState.OPEN);
            log.warn("半开状态调用失败，熔断器重新打开（OPEN）");
        }
    }

    /**
     * 状态转换，同时重置相关计数器 —— "切换保险丝的工作模式"
     *
     * <p>synchronized 保证多线程环境下状态转换的原子性：
     * 防止两个线程同时检测到超时并尝试转换状态导致的竞态条件。</p>
     *
     * <p>每次状态转换时，根据目标状态重置对应的计数器：
     * <ul>
     *   <li>→ OPEN：记录进入时间戳，重置成功计数（为超时判断提供基准）</li>
     *   <li>→ CLOSED：重置失败和成功计数（重新开始计数）</li>
     *   <li>→ HALF_OPEN：重置成功计数（准备接受试探性调用的结果）</li>
     * </ul>
     *
     * @param newState 目标状态
     */
    private synchronized void transitionTo(CircuitBreakerState newState) {
        state.set(newState);
        if (newState == CircuitBreakerState.OPEN) {
            // 进入熔断状态：记录开始时间，重置成功计数
            openTimestamp = System.currentTimeMillis();
            successCount.set(0);
        } else if (newState == CircuitBreakerState.CLOSED) {
            // 恢复正常状态：清零所有计数器，重新开始
            failureCount.set(0);
            successCount.set(0);
        } else if (newState == CircuitBreakerState.HALF_OPEN) {
            // 进入半开状态：重置成功计数，准备接受试探性调用
            successCount.set(0);
        }
    }

    @Override
    public CircuitBreakerState getState() {
        return state.get();
    }

    @Override
    public void reset() {
        transitionTo(CircuitBreakerState.CLOSED);
        log.info("熔断器已手动重置为 CLOSED");
    }

    /**
     * 获取当前连续失败次数（用于监控/调试）。
     */
    public int getFailureCount() {
        return failureCount.get();
    }
}
