package com.hgl.hglrpc.fault.tolerant;

import com.hgl.hglrpc.model.RpcResponse;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * @ClassName: FailSafeTolerantStrategy
 * @Package: com.hgl.hglrpc.fault.tolerant
 * @Description: 静默处理异常 - 容错策略
 * @Author HGL
 * @Create: 2025/9/5 14:32
 */
@Slf4j
public class FailSafeTolerantStrategy implements TolerantStrategy {
    @Override
    public RpcResponse doTolerant(Map<String, Object> context, Exception e) {
        log.info("静默处理异常", e);
        return new RpcResponse();
    }
}
