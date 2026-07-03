package com.hgl.hglrpc.server.http;

import com.hgl.hglrpc.RpcApplication;
import com.hgl.hglrpc.model.RpcRequest;
import com.hgl.hglrpc.model.RpcResponse;
import com.hgl.hglrpc.registry.LocalRegistry;
import com.hgl.hglrpc.serializer.Serializer;
import com.hgl.hglrpc.serializer.SerializerFactory;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.http.HttpServerResponse;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

/**
 * HTTP 快递员的派件流程 —— 处理每一个 HTTP 请求
 *
 * <p>HttpServerHandler 实现了 Vert.x 的 {@code Handler<HttpServerRequest>} 接口，
 * 每当有 HTTP 请求到达时，Vert.x 会调用 handle() 方法。
 *
 * <p>与 TCP 版本 ({@link com.hgl.hglrpc.server.tcp.TcpServerHandler}) 相比，
 * HTTP 版本不需要粘包拆包处理器和自定义协议编解码器，
 * 因为 HTTP 协议本身就是自描述的，请求边界天然清晰。
 *
 * <p>HTTP 版的派件流程：
 * <pre>
 *   ┌────────────────────────────────────────────────────────────────────────┐
 *   │                    HTTP 快递员的派件流程                                │
 *   │                                                                        │
 *   │  客户端 ──── HTTP POST ────▶ HttpServerRequest                        │
 *   │                               │                                        │
 *   │                               ▼                                        │
 *   │                   request.bodyHandler(body -> { ... })                 │
 *   │                   （等请求体全部到达后再处理）                          │
 *   │                               │                                        │
 *   │                               ▼                                        │
 *   │                   serializer.deserialize(body, RpcRequest.class)       │
 *   │                   （拆包裹：从 HTTP body 反序列化出 RpcRequest）        │
 *   │                               │                                        │
 *   │                               ▼                                        │
 *   │                   LocalRegistry.get(serviceName)                       │
 *   │                   （查快递柜：找到目标服务的实现类）                     │
 *   │                               │                                        │
 *   │                               ▼                                        │
 *   │                   method.invoke(serviceInstance, args)                 │
 *   │                   （送货上门：通过反射调用目标方法）                     │
 *   │                               │                                        │
 *   │                               ▼                                        │
 *   │                   serializer.serialize(rpcResponse)                    │
 *   │                   （封装回信：序列化 RpcResponse）                      │
 *   │                               │                                        │
 *   │                               ▼                                        │
 *   │                   response.end(Buffer.buffer(serialized))              │
 *   │                   （发回签收单：HTTP 响应）                             │
 *   └────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/8/29 16:23
 * @see com.hgl.hglrpc.server.tcp.TcpServerHandler TCP 版快递员（对比参考）
 * @see LocalRegistry 本地服务注册表
 * @see SerializerFactory 序列化工厂
 */
@Slf4j
public class HttpServerHandler implements Handler<HttpServerRequest> {

    /**
     * 处理 HTTP 请求 —— "HTTP 快递员接单派件"
     *
     * <p>整体流程：读取请求体 → 反序列化 → 反射调用 → 序列化响应 → 返回
     *
     * @param request Vert.x 封装的 HTTP 请求对象
     */
    @Override
    public void handle(HttpServerRequest request) {
        // 获取序列化器 —— "这封信是用什么语言写的？从配置中读取"
        final Serializer serializer = SerializerFactory.getInstance(RpcApplication.getRpcConfig().getSerializer());

        log.info("Received request: {} {}", request.method(), request.uri());

        // 异步读取请求体 —— "等包裹全部到齐再拆"
        // HTTP 请求体可能分多个 TCP 包到达，bodyHandler 会等全部到齐后回调
        request.bodyHandler(body -> {
            // ========== 第1步：拆包裹 —— 反序列化请求体 ==========
            byte[] bytes = body.getBytes();
            RpcRequest rpcRequest = serializer.deserialize(bytes, RpcRequest.class);

            // 构造响应结果 —— 准备"签收回执"
            RpcResponse rpcResponse = new RpcResponse();

            if (rpcRequest == null) {
                // 空请求 —— "包裹里啥也没有"
                rpcResponse.setMessage("rpcRequest is null");
                doResponse(request, rpcResponse, serializer);
                return;
            }

            // ========== 第2步：处理请求 —— 查快递柜、送货上门 ==========
            try {
                // 从本地注册表查找服务实现 —— "查快递柜，找到收件地址"
                Object serviceInstance = LocalRegistry.get(rpcRequest.getServiceName());
                if (serviceInstance == null) {
                    throw new RuntimeException("未找到服务: " + rpcRequest.getServiceName());
                }

                // 通过反射调用目标方法 —— "送货上门"
                Method method = serviceInstance.getClass().getMethod(
                        rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
                Object result = method.invoke(serviceInstance, rpcRequest.getArgs());

                // 设置响应结果 —— "对方签收了"
                rpcResponse.setData(result);
                rpcResponse.setDataType(method.getReturnType());
                rpcResponse.setMessage("ok");
            } catch (Exception e) {
                // 调用出错 —— "包裹损坏或地址有误"
                log.error("Error: ", e);
                rpcResponse.setMessage(e.getMessage());
                rpcResponse.setException(e);
            }

            // ========== 第3步：返回响应 —— "发回签收回执" ==========
            doResponse(request, rpcResponse, serializer);
        });
    }

    /**
     * 发送 HTTP 响应 —— "把签收回执寄回去"
     *
     * <p>将 RpcResponse 序列化后写入 HTTP 响应体返回给客户端。
     *
     * <p>注意：无论业务逻辑成功还是失败，都会返回 HTTP 200 状态码，
     * 具体的业务状态通过 RpcResponse 中的 message 和 exception 字段传递。
     * 这是 RPC 框架的常见做法——HTTP 状态码只反映"通信是否正常"，
     * 而不是"业务是否成功"。
     *
     * @param request      原始 HTTP 请求
     * @param rpcResponse  RPC 响应结果
     * @param serializer   序列化器
     */
    private void doResponse(HttpServerRequest request, RpcResponse rpcResponse, Serializer serializer) {
        // 设置响应头 —— 告诉对方"回执是用 JSON 写的"
        HttpServerResponse httpServerResponse = request.response()
                .putHeader("content-type", "application/json");
        try {
            // 序列化响应 —— "把签收回执折好放进信封"
            byte[] serialized = serializer.serialize(rpcResponse);
            httpServerResponse.end(Buffer.buffer(serialized));
        } catch (Exception e) {
            // 序列化失败 —— "写回执的时候笔没墨了"
            log.error("Error: ", e);
            httpServerResponse.end(Buffer.buffer());
        }
    }
}
