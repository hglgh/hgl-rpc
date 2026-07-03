package com.hgl.hglrpc.fault.tolerant;

import com.hgl.hglrpc.model.RpcResponse;

import java.util.Map;

/**
 * 快速失败容错策略 —— 「送不到就送不到，直接告诉客户！」
 *
 * <p>这是最「诚实」的容错策略：一旦调用失败，立刻把异常抛给上层调用方，
 * 不做任何补救。就像快递员发现包裹寄不了，直接打电话告诉你：
 * 「对不起，您的包裹送不了，原因如下……」</p>
 *
 * <p><b>核心理念：Fail Fast, Fail Loud</b></p>
 * <p>快速失败策略强调的是「问题要尽早暴露」。与其默默吞掉错误导致
 * 更大的问题（比如用户以为操作成功了），不如直接炸掉，让开发人员
 * 能第一时间发现并修复。</p>
 *
 * <pre>
 * 调用流程：
 *
 *   RPC 调用失败
 *        │
 *        ▼
 *   ┌──────────────┐
 *   │  FailFast     │
 *   │  快速失败策略  │
 *   └──────┬───────┘
 *          │
 *          ▼
 *   直接抛出 RuntimeException
 *   "服务报错"
 *
 *   特点：零延迟、零容忍
 * </pre>
 *
 * <p><b>适用场景：</b></p>
 * <ul>
 *   <li>支付、转账等关键业务（不能静悄悄地失败）</li>
 *   <li>数据一致性要求高的场景</li>
 *   <li>调试阶段（需要第一时间看到错误信息）</li>
 * </ul>
 *
 * @author HGL
 * @since 2025/9/5
 */
public class FailFastTolerantStrategy implements TolerantStrategy {

    /**
     * 快速失败：直接将异常向上抛出。
     *
     * <p>这个方法什么都不做补救，只是简单地把异常重新包装成 RuntimeException 抛出去。
     * 就像快递员说「我尽力了，但真的送不了，你自己看着办吧」。</p>
     *
     * @param context 上下文信息（本策略不使用，因为不需要额外信息就能报错）
     * @param e       原始异常
     * @return RpcResponse 永远不会返回，因为方法内部直接抛异常了
     * @throws RuntimeException 包装后的运行时异常
     */
    @Override
    public RpcResponse doTolerant(Map<String, Object> context, Exception e) {
        // 毫不犹豫：出了问题就告诉外面，绝不藏着掖着
        throw new RuntimeException("服务报错", e);
    }
}
