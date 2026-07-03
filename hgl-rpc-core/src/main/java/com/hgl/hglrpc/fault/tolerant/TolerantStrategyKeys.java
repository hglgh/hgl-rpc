package com.hgl.hglrpc.fault.tolerant;

/**
 * 容错策略的编号（SPI 键名常量）
 *
 * <p>每种容错策略都有一个「身份证号码」，这个接口就是花名册。
 * 通过 SPI 机制，系统可以根据这些 key 动态加载对应的策略实现，
 * 实现「策略可配置、可插拔」的效果。</p>
 *
 * <p>就像外卖 App 的骑手异常处理策略：
 * <ul>
 *   <li>failFast —— 骑手送不了就直接取消订单</li>
 *   <li>failOver —— 骑手送不了就换一个骑手</li>
 *   <li>failSafe —— 骑手送不了就算了，不收你钱</li>
 *   <li>failBack —— 骑手送不了就放到最近的自提点</li>
 * </ul>
 * </p>
 *
 * <pre>
 * 策略选择示意：
 *
 *   配置 key       对应实现类                      一句话概括
 *   ──────────    ──────────────────────────     ──────────────
 *   "failFast"    FailFastTolerantStrategy       出错就报，绝不姑息
 *   "failOver"    FailOverTolerantStrategy       这台不行换那台
 *   "failSafe"    FailSafeTolerantStrategy       出错也当没事发生
 *   "failBack"    FailBackTolerantStrategy       正路不通走小路
 * </pre>
 *
 * @author HGL
 * @since 2025/9/5
 * @see TolerantStrategyFactory 容错策略工厂，使用这些 key 来获取实例
 */
public interface TolerantStrategyKeys {

    /**
     * 失败回调（降级）策略的键名。
     *
     * <p>对应 {@link FailBackTolerantStrategy}，调用失败后降级到 Mock 服务。
     * 就像快递送不了时放到最近的自提点。</p>
     */
    String FAIL_BACK = "failBack";

    /**
     * 快速失败策略的键名。
     *
     * <p>对应 {@link FailFastTolerantStrategy}，调用失败直接抛异常。
     * 就像快递送不了就直接告诉你「送不了」。</p>
     */
    String FAIL_FAST = "failFast";

    /**
     * 故障转移策略的键名。
     *
     * <p>对应 {@link FailOverTolerantStrategy}，调用失败后转移到其他节点重试。
     * 就像这个骑手不行换一个骑手。</p>
     */
    String FAIL_OVER = "failOver";

    /**
     * 安全失败（静默处理）策略的键名。
     *
     * <p>对应 {@link FailSafeTolerantStrategy}，调用失败后默默返回空结果。
     * 就像快递送不了就算了，默默不提。</p>
     */
    String FAIL_SAFE = "failSafe";
}
