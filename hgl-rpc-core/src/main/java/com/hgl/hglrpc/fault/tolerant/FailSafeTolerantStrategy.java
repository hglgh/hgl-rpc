package com.hgl.hglrpc.fault.tolerant;

import com.hgl.hglrpc.model.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 安全失败容错策略 —— 「出错了？没事，假装什么都没发生」
 *
 * <p>当调用失败时，默默吞掉异常，返回一个空的默认响应。
 * 就像快递员送错了地址，他没有声张，而是悄悄给你放了个空包裹在门口，
 * 你打开一看——啥也没有，但至少没收到一个爆炸包裹。</p>
 *
 * <p><b>核心理念：Fail Safe</b></p>
 * <p>安全失败策略的重点是「保证系统不崩溃」。有些非关键业务
 * （比如推荐系统、广告展示、日志记录）就算失败了也不应该影响
 * 主流程，这时候就适合用 FailSafe。</p>
 *
 * <pre>
 * 调用流程：
 *
 *   RPC 调用失败
 *        │
 *        ▼
 *   ┌──────────────┐
 *   │  FailSafe     │
 *   │  安全失败策略  │
 *   └──────┬───────┘
 *          │
 *          ▼
 *   1. 记录日志（log.info）   ◀── 「悄悄记下来，别惊动用户」
 *          │
 *          ▼
 *   2. 返回空的 RpcResponse    ◀── 「给你个空包裹，好歹没出错」
 *
 *   特点：静默、不报错、不打扰
 * </pre>
 *
 * <p><b>适用场景：</b></p>
 * <ul>
 *   <li>非关键业务：推荐内容、广告、统计上报</li>
 *   <li>降级场景：核心服务挂了，辅助功能静默失败</li>
 *   <li>广播通知：发通知失败了，不影响主流程</li>
 * </ul>
 *
 * <p><b>风险提示：</b>过度使用 FailSafe 可能掩盖真正的问题，
 * 就像一直在吃止痛药而不去治病。建议配合监控告警使用。</p>
 *
 * @author HGL
 * @since 2025/9/5
 */
@Slf4j
public class FailSafeTolerantStrategy implements TolerantStrategy {

    /**
     * 安全失败：记录日志后返回空响应。
     *
     * <p>就像一个脾气特别好的快递员，送失败了也不生气，
     * 默默记了个笔记就回去交差了。</p>
     *
     * @param context 上下文信息（本策略不使用额外上下文，因为只是静默处理）
     * @param e       原始异常（会被记录到日志中，方便事后排查）
     * @return RpcResponse 空的响应对象（不包含有效数据）
     */
    @Override
    public RpcResponse doTolerant(Map<String, Object> context, Exception e) {
        // 静默记录异常信息（不是吞掉，而是「有迹可查」）
        log.info("静默处理异常", e);
        // 返回空响应，调用方拿到后要做空值判断
        return new RpcResponse();
    }
}
