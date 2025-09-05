package com.hgl.hglrpc.fault.tolerant;

import com.hgl.hglrpc.model.RpcResponse;

import java.util.Map;

/**
 * @ClassName: TolerantStrategy
 * @Package: com.hgl.hglrpc.fault.tolerant
 * @Description: 容错策略
 * @Author HGL
 * @Create: 2025/9/5 14:26
 */
public interface TolerantStrategy {
    /**
     * 容错
     *
     * @param context 上下文，用于传递数据
     * @param e       异常
     * @return RpcResponse
     */
    RpcResponse doTolerant(Map<String, Object> context, Exception e);
}
