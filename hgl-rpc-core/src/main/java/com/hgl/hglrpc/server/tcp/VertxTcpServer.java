package com.hgl.hglrpc.server.tcp;

import com.hgl.hglrpc.server.VertxServer;
import io.vertx.core.Vertx;
import io.vertx.core.net.NetServer;
import lombok.extern.slf4j.Slf4j;

/**
 * TCP 快递网点 —— 基于 Vert.x 的 TCP 服务器实现
 *
 * <p>VertxTcpServer 是 {@link VertxServer} 接口的 TCP 实现，
 * 它启动一个基于 Vert.x 的 TCP 服务器，监听指定端口，等待客户端的二进制 RPC 请求。
 *
 * <p>整个处理流程就像快递网点的工作流水线：
 * <pre>
 *   ┌─────────────────────────────────────────────────────────────────────────┐
 *   │                    TCP 快递网点（VertxTcpServer）                          │
 *   │                                                                         │
 *   │  1. 创建 Vert.x 实例（租场地、招人）                                         │
 *   │         │                                                               │
 *   │         ▼                                                               │
 *   │  2. 创建 NetServer（挂上"XX快递网点"的招牌）                                 │
 *   │         │                                                               │
 *   │         ▼                                                               │
 *   │  3. 注册 TcpServerHandler（安排"快递员"上岗）                                │
 *   │         │                                                               │
 *   │         ▼                                                               │
 *   │  4. 监听端口（开门营业，等待包裹上门）                                         │
 *   │         │                                                               │
 *   │         ▼                                                               │
 *   │  ┌─────────────────────────────────────────────────────────────┐        │
 *   │  │  有客户端连接进来！TcpServerHandler 开始派件：                    │        │
 *   │  │                                                             │        │
 *   │  │  收到二进制数据                                                │        │
 *   │  │       │                                                     │        │
 *   │  │       ▼                                                     │        │
 *   │  │  TcpBufferHandlerWrapper（粘包拆包 → 还原完整信封）              │        │
 *   │  │       │                                                     │        │
 *   │  │       ▼                                                     │        │
 *   │  │  ProtocolMessageDecoder（拆信封 → 读出 RpcRequest）            │        │
 *   │  │       │                                                     │        │
 *   │  │       ▼                                                     │        │
 *   │  │  LocalRegistry 查找服务实现 → 反射调用目标方法                    │        │
 *   │  │       │                                                     │        │
 *   │  │       ▼                                                     │        │
 *   │  │  ProtocolMessageEncoder（封装回信 → 编码为字节流）               │        │
 *   │  │       │                                                     │        │
 *   │  │       ▼                                                     │        │
 *   │  │  写回客户端 socket                                            │        │
 *   │  └─────────────────────────────────────────────────────────────┘        │
 *   └─────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>为什么用 Vert.x 的 NetServer 而不是 JDK 原生 ServerSocket？
 * <ul>
 *   <li>Vert.x 基于 Netty，天然异步非阻塞，一个线程可以处理成千上万的连接</li>
 *   <li>内置事件循环模型，避免了传统 BIO "一连接一线程"的资源浪费</li>
 *   <li>API 简洁，只需关注 Handler 回调，不需要手动管理线程池</li>
 * </ul>
 *
 * @Author HGL
 * @Create: 2025/9/3 15:05
 * @see TcpServerHandler TCP 快递员，负责具体的请求处理逻辑
 * @see TcpBufferHandlerWrapper 粘包拆包处理器
 */
@Slf4j
public class VertxTcpServer implements VertxServer {

    /**
     * 启动 TCP 服务器 —— "快递网点开门营业"
     *
     * <p>创建 Vert.x 实例、NetServer，并绑定端口开始监听。
     * 每当有新的客户端 TCP 连接建立时，{@link TcpServerHandler} 会被调用，
     * 就像网点门口来了一个"取件人"，快递员开始为其服务。
     *
     * @param port 监听的端口号，即快递网点的"门牌号"
     */
    @Override
    public void doStart(int port) {
        // 创建 Vert.x 实例 —— 租场地、招人、组建团队
        Vertx vertx = Vertx.vertx();

        // 创建 TCP 服务器 —— 挂上"快递网点"的招牌
        NetServer server = vertx.createNetServer();

        // 注册连接处理器 —— 安排快递员（TcpServerHandler）上岗
        // 每当有新的客户端连接进来，就会创建一个 TcpServerHandler 来处理
        server.connectHandler(new TcpServerHandler());

        // 启动 TCP 服务器并监听指定端口 —— 开门营业！
        server.listen(port, result -> {
            if (result.succeeded()) {
                log.info("TCP server started on port {}", port);
            } else {
                log.error("Failed to start TCP server: {}", result.cause().getMessage());
            }
        });
    }
}
