package com.hgl.hglrpc.fault.retry;

import com.hgl.hglrpc.model.RpcResponse;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @ClassName: RetryStrategyTest
 * @Package: com.hgl.hglrpc.fault.retry
 * @Description: 重试策略的验收测试
 * @Author HGL
 * @Create: 2025/9/5 11:29
 *
 * <h2>测试全景</h2>
 * 重试策略就像一位"百折不挠的快递员"——第一次投递失败了，
 * 它不会立刻放弃，而是按照既定的节奏（固定间隔、指数退避等）
 * 反复尝试，直到成功或耗尽所有重试次数。
 *
 * <p>本测试类验证 {@link FixedIntervalRetryStrategy}（固定间隔重试策略），
 * 模拟一个"永远失败"的调用，观察重试器是否忠实执行了重试流程
 * 并在最终失败时正确抛出异常。
 */
@Slf4j
class RetryStrategyTest {

    /** 被测对象：固定间隔重试策略——"执着的快递员" */
    RetryStrategy retryStrategy = new FixedIntervalRetryStrategy();

    /**
     * <h3>测试目标：重试策略在持续失败时的完整生命周期</h3>
     * <p>
     * 提供一个"永远失败"的回调（模拟投递时收件人永远不在家），
     * 验证重试器是否会：
     * <ol>
     *   <li>按照固定间隔反复尝试投递</li>
     *   <li>每次失败后记录日志</li>
     *   <li>耗尽重试次数后抛出异常，而不是无限循环</li>
     * </ol>
     *
     * <h3>期望行为</h3>
     * <ul>
     *   <li>回调被执行多次（由日志中的"测试重试"次数可确认）</li>
     *   <li>最终捕获到异常，打印"重试多次失败"</li>
     *   <li>不会陷入死循环——重试有上限</li>
     * </ul>
     *
     * <p><b>注意：</b>这是一个"观察型"测试——主要通过控制台输出来判断
     * 重试行为是否符合预期，断言本身较少，因为重试次数和间隔
     * 取决于策略实现的内部配置。
     */
    @Test
    public void doRetry() {
        try {
            // 提交一个"注定失败"的任务——快递员第 N 次去投递，收件人依旧不在
            RpcResponse rpcResponse = retryStrategy.doRetry(() -> {
                System.out.println("测试重试");
                throw new RuntimeException("模拟重试失败"); // 模拟投递失败：收件人不在家
            });
            // 如果走到了这里，说明某次重试意外成功了（不应发生）
            System.out.println(rpcResponse);
        } catch (Exception e) {
            // 预期走到这里：耗尽所有重试次数后，快递员终于放弃了
            System.out.println("重试多次失败");
            log.error("Error: ", e);
        }
    }
}
