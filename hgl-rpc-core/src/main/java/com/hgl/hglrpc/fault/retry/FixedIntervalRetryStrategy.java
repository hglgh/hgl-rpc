package com.hgl.hglrpc.fault.retry;

import com.github.rholder.retry.*;
import com.hgl.hglrpc.model.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * 固定间隔重试策略 —— 「失败了？等一会儿再试试」
 *
 * <p>当 RPC 调用失败时，不是立刻放弃，而是每隔固定的时间（本实现为 3 秒）再试一次，
 * 最多重试 3 次。就像你去餐厅吃饭，没开门就等 3 分钟再去，最多等 3 次。</p>
 *
 * <p><b>核心参数：</b></p>
 * <ul>
 *   <li>重试间隔: 3 秒（每次失败后等 3 秒再试）</li>
 *   <li>最大重试次数: 3 次（加上首次调用共 4 次机会）</li>
 *   <li>重试条件: 遇到任何 Exception 都重试</li>
 * </ul>
 *
 * <pre>
 * 固定间隔 vs 指数退避（对比）：
 *
 * ┌─────────────────────────────────────────────────────┐
 * │  固定间隔（本策略）                                    │
 * │                                                      │
 * │  时间轴:  0s     3s      6s      9s                  │
 * │           │      │       │       │                   │
 * │          第1次  第2次   第3次   第4次(放弃)             │
 * │          请求   请求    请求     抛异常                 │
 * │           ▲      ▲       ▲                           │
 * │           └──────┴───────┘                           │
 * │           每次间隔都是 3 秒（像时钟一样规律）             │
 * │                                                      │
 * │  优点：简单可预测                                      │
 * │  缺点：如果服务端很忙，固定轰炸可能雪上加霜              │
 * └─────────────────────────────────────────────────────┘
 *
 * ┌─────────────────────────────────────────────────────┐
 * │  指数退避（ExponentialBackoff，如果有的话）              │
 * │                                                      │
 * │  时间轴:  0s    2s       6s            14s            │
 * │           │     │        │              │             │
 * │          第1次 第2次    第3次          第4次(放弃)       │
 * │                                                      │
 * │  间隔:     2s    4s       8s（每次翻倍）                │
 * │                                                      │
 * │  优点：给服务端更多恢复时间                              │
 * │  缺点：实现稍复杂，等待时间可能过长                      │
 * └─────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p><b>使用 guava-retrying 库</b>来实现重试逻辑，这个库封装了重试的各种模式，
 * 就像用瑞士军刀而不是自己削木头做工具。</p>
 *
 * @author HGL
 * @since 2025/9/5
 * @see RetryStrategy 重试策略接口
 * @see NoRetryStrategy 对比：不重试策略
 */
@Slf4j
public class FixedIntervalRetryStrategy implements RetryStrategy {

    /**
     * 使用固定间隔执行重试。
     *
     * <p>内部使用 guava-retrying 库构建一个重试器 {@link Retryer}，
     * 配置如下：
     * <ol>
     *   <li>遇到 Exception 就触发重试</li>
     *   <li>每次重试等待 3 秒</li>
     *   <li>最多尝试 3 次后停止（不再重试）</li>
     *   <li>每次重试时记录日志</li>
     * </ol>
     * </p>
     *
     * @param callable 要执行的 RPC 调用
     * @return RpcResponse 最终调用结果
     * @throws Exception 所有重试都失败后抛出最后一次的异常
     */
    @Override
    public RpcResponse doRetry(Callable<RpcResponse> callable) throws Exception {

        // 构建重试器：像给快递员制定了一份详细的「重送包裹守则」
        Retryer<RpcResponse> retryer = RetryerBuilder.<RpcResponse>newBuilder()
                // 规则1：遇到任何异常都要重试（不是说 "算了不送了"）
                .retryIfExceptionOfType(Exception.class)
                // 规则2：每次重试之间等 3 秒（不急，给人家一点处理时间）
                .withWaitStrategy(WaitStrategies.fixedWait(3L, TimeUnit.SECONDS))
                // 规则3：最多尝试 3 次就收手（总不能一直送下去吧）
                .withStopStrategy(StopStrategies.stopAfterAttempt(3))
                // 监听器：每次重试时记录一下日志，方便排查问题
                .withRetryListener(new RetryListener() {
                    @Override
                    public <V> void onRetry(Attempt<V> attempt) {
                        long attemptNumber = attempt.getAttemptNumber();
                        // 第一次是正常调用，不算重试，从第 2 次开始记录
                        if (attemptNumber > 1) {
                            log.info("重试次数 {}", attemptNumber - 1);
                        }
                    }
                })
                .build();

        // 启动重试器，让它按照上面的规则执行
        return retryer.call(callable);
    }
}
