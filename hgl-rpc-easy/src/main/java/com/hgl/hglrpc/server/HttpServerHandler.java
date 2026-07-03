package com.hgl.hglrpc.server;

import com.hgl.hglrpc.model.RpcRequest;
import com.hgl.hglrpc.model.RpcResponse;
import com.hgl.hglrpc.registry.LocalRegistry;
import com.hgl.hglrpc.serializer.JdkSerializer;
import com.hgl.hglrpc.serializer.Serializer;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

/**
 * HTTP 请求处理器 —— "快递员的派件流程"（easy 模块简化版）
 *
 * <p>收到 HTTP 请求后的完整处理流程：
 * <pre>
 *   1. 接收 HTTP 请求，读取请求体（字节数组）
 *   2. 反序列化为 RpcRequest 对象（拆开快递包装）
 *   3. 根据 serviceName 从 LocalRegistry 获取服务实例
 *   4. 通过反射找到目标方法并调用（办理业务）
 *   5. 将结果封装为 RpcResponse 并序列化返回
 *
 *   ┌──────────┐     ┌──────────┐     ┌───────────┐     ┌──────────┐
 *   │ HTTP 请求 │ →→→ │ 反序列化 │ →→→ │ 反射调用  │ →→→ │ 返回结果 │
 *   └──────────┘     └──────────┘     └───────────┘     └──────────┘
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/8/29 16:23
 */
@Slf4j
public class HttpServerHandler implements Handler<HttpServerRequest> {

    @Override
    public void handle(HttpServerRequest request) {
        // 使用 JDK 序列化器（easy 模块简化方案）
        JdkSerializer jdkSerializer = new JdkSerializer();
        log.info("Received request: {} {}", request.method(), request.uri());

        // 异步读取请求体（Vert.x 的异步回调模式）
        request.bodyHandler(body -> {
            byte[] bytes = body.getBytes();
            // 1. 反序列化：字节数组 → RpcRequest 对象
            RpcRequest rpcRequest = jdkSerializer.deserialize(bytes, RpcRequest.class);

            // 构造响应对象
            RpcResponse rpcResponse = new RpcResponse();
            if (rpcRequest == null) {
                rpcResponse.setMessage("rpcRequest is null");
                doResponse(request, rpcResponse, jdkSerializer);
                return;
            }

            // 2. 服务调用：通过反射执行目标方法
            try {
                // 从本地注册表获取服务实例
                Object serviceInstance = LocalRegistry.get(rpcRequest.getServiceName());
                if (serviceInstance == null) {
                    throw new RuntimeException("未找到服务: " + rpcRequest.getServiceName());
                }
                // 通过反射找到并调用方法
                Method method = serviceInstance.getClass()
                        .getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
                Object result = method.invoke(serviceInstance, rpcRequest.getArgs());

                // 3. 封装成功响应
                rpcResponse.setData(result);
                rpcResponse.setDataType(method.getReturnType());
                rpcResponse.setMessage("ok");
            } catch (Exception e) {
                log.error("Error: ", e);
                // 4. 封装失败响应
                rpcResponse.setMessage(e.getMessage());
                rpcResponse.setException(e);
            }

            // 5. 序序化响应并返回
            doResponse(request, rpcResponse, jdkSerializer);
        });
    }

    /**
     * 发送响应 —— "把回执单寄回去"
     *
     * @param request      原始请求
     * @param rpcResponse  响应对象
     * @param serializer   序列化器
     */
    private void doResponse(HttpServerRequest request, RpcResponse rpcResponse, Serializer serializer) {
        HttpServerResponse httpServerResponse = request.response()
                .putHeader("content-type", "application/json");
        try {
            // 序列化响应对象为字节数组
            byte[] serialized = serializer.serialize(rpcResponse);
            httpServerResponse.end(Buffer.buffer(serialized));
        } catch (Exception e) {
            log.error("Error: ", e);
            httpServerResponse.end(Buffer.buffer());
        }
    }
}
