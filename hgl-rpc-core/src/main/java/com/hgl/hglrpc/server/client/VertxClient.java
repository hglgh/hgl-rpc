package com.hgl.hglrpc.server.client;

import com.hgl.hglrpc.model.RpcRequest;
import com.hgl.hglrpc.model.RpcResponse;
import com.hgl.hglrpc.model.ServiceMetaInfo;

import java.util.concurrent.CompletableFuture;

/**
 * 客户端的工作规范 —— "快递员的岗位职责"
 *
 * <p>VertxClient 是所有 RPC 客户端的顶层接口，就像快递员的《岗位职责手册》——
 * 不管你是"电动车快递员"（TCP）还是"同城配送员"（HTTP），
 * 都必须完成同一个核心任务：<b>拿着包裹（RpcRequest），送到指定地址（ServiceMetaInfo），取回签收单（RpcResponse）。</b>
 *
 * <p>接口定义了两种调用方式：
 * <pre>
 *   ┌──────────────────────────────────────────────────────────────┐
 *   │                      VertxClient 接口                         │
 *   │               （快递员的统一岗位职责）                            │
 *   │                                                              │
 *   │    +doRequest(request, metaInfo): RpcResponse                │
 *   │      │                                                       │
 *   │      ├── 同步阻塞调用，等待响应后返回                             │
 *   │      └── 适用于简单场景，调用方希望直接拿到结果                     │
 *   │                                                              │
 *   │    +doRequestAsync(request, metaInfo): CompletableFuture     │
 *   │      │                                                       │
 *   │      ├── 异步非阻塞调用，立即返回 Future                          │
 *   │      └── 适用于并发场景，不阻塞当前线程                            │
 *   │                                                              │
 *   │         ┌─────────────────────┬─────────────────────┐        │
 *   │         ▼                     ▼                     │        │
 *   │  ┌──────────────┐    ┌───────────────┐              │        │
 *   │  │VertxTcpClient│    │VertxHttpClient│    ...       │        │
 *   │  │（TCP 快递员）  │    │（HTTP 快递员）  │              │        │
 *   │  └──────────────┘    └───────────────┘              │        │
 *   └──────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/9/4 15:18
 * @see VertxClientFactory 客户端工厂，通过 SPI 机制按名称获取具体实现
 * @see com.hgl.hglrpc.server.tcp.VertxTcpClient TCP 快递员
 */
public interface VertxClient {

    /**
     * 发送 RPC 请求 —— "送快递并取回签收单"
     *
     * <p>将 RpcRequest 发送到目标服务，并等待返回 RpcResponse。
     * 这是一个<b>同步阻塞</b>方法——快递员必须等到对方签收（或超时）才会回来。
     *
     * @param rpcRequest      请求参数，即要发送的"包裹内容"
     * @param serviceMetaInfo 服务元信息，即收件人的"地址"（host + port + serviceName 等）
     * @return RpcResponse 响应结果，即对方的"签收回执"
     * @throws Throwable 网络异常、序列化异常、超时等任何可能的错误
     */
    RpcResponse doRequest(RpcRequest rpcRequest, ServiceMetaInfo serviceMetaInfo) throws Throwable;

    /**
     * 异步发送 RPC 请求 —— "寄快递并拿到取件码，不必原地等"
     *
     * <p>与 {@link #doRequest} 相同的逻辑，但以异步方式返回结果。
     * 调用方可以通过 CompletableFuture 的 thenAccept/thenApply 等回调处理结果，
     * 不阻塞当前线程。
     *
     * <p>默认实现：将同步调用包装到 CompletableFuture.supplyAsync 中。
     * 子类可以覆写为真正的异步实现以获得更好性能。
     *
     * @param rpcRequest      请求参数
     * @param serviceMetaInfo 服务元信息
     * @return 包含 RpcResponse 的 CompletableFuture
     */
    default CompletableFuture<RpcResponse> doRequestAsync(RpcRequest rpcRequest, ServiceMetaInfo serviceMetaInfo) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return doRequest(rpcRequest, serviceMetaInfo);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        });
    }
}
