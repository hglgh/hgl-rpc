package com.hgl.hglrpc.server.client;

import com.hgl.hglrpc.model.RpcRequest;
import com.hgl.hglrpc.model.RpcResponse;
import com.hgl.hglrpc.model.ServiceMetaInfo;

/**
 * @ClassName: VertxClient
 * @Package: com.hgl.hglrpc.server
 * @Description: vertx 客户端
 * @Author HGL
 * @Create: 2025/9/4 15:18
 */
public interface VertxClient {
    /**
     * 请求服务
     *
     * @param rpcRequest      请求参数
     * @param serviceMetaInfo 服务信息
     * @return 请求结果
     * @throws Throwable 抛出异常
     */
    RpcResponse doRequest(RpcRequest rpcRequest, ServiceMetaInfo serviceMetaInfo) throws Throwable;
}
