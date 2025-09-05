package com.hgl.hglrpc.fault.tolerant;

import com.hgl.hglrpc.model.RpcResponse;

import java.util.Map;

/**
 * @ClassName: FailFastTolerantStrategy
 * @Package: com.hgl.hglrpc.fault.tolerant
 * @Description: 快速失败 - 容错策略（立刻通知外层调用方）
 * @Author HGL
 * @Create: 2025/9/5 14:30
 */
public class FailFastTolerantStrategy implements TolerantStrategy {
    @Override
    public RpcResponse doTolerant(Map<String, Object> context, Exception e) {
        throw new RuntimeException("服务报错", e);
    }
}
