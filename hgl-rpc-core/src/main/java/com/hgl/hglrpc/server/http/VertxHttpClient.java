package com.hgl.hglrpc.server.http;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.hgl.hglrpc.RpcApplication;
import com.hgl.hglrpc.model.RpcRequest;
import com.hgl.hglrpc.model.RpcResponse;
import com.hgl.hglrpc.model.ServiceMetaInfo;
import com.hgl.hglrpc.serializer.Serializer;
import com.hgl.hglrpc.serializer.SerializerFactory;
import com.hgl.hglrpc.server.client.VertxClient;
import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName: VertxHttpClient
 * @Package: com.hgl.hglrpc.server.http
 * @Description:
 * @Author HGL
 * @Create: 2025/9/4 15:27
 */
@Slf4j
public class VertxHttpClient implements VertxClient {
    @Override
    public RpcResponse doRequest(RpcRequest rpcRequest, ServiceMetaInfo serviceMetaInfo) throws Throwable {
        log.info("http client send request to server");
        final Serializer serializer = SerializerFactory.getInstance(RpcApplication.getRpcConfig().getSerializer());
        byte[] bodyBytes = serializer.serialize(rpcRequest);
        try (HttpResponse httpResponse = HttpRequest.post(serviceMetaInfo.getServiceAddress())
                .body(bodyBytes)
                .execute()) {
            byte[] result = httpResponse.bodyBytes();
            // 反序列化
            return serializer.deserialize(result, RpcResponse.class);
        }
    }
}
