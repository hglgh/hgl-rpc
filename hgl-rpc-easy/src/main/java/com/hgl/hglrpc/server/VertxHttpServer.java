package com.hgl.hglrpc.server;

import io.vertx.core.Vertx;
import lombok.extern.slf4j.Slf4j;

/**
 * 基于 Vert.x 的 HTTP 服务器实现 —— "简易快递网点"（easy 模块简化版）
 *
 * <p>使用 Vert.x 创建 HTTP 服务器，监听指定端口。
 * 收到请求后交给 {@link HttpServerHandler} 处理。
 *
 * <p>Vert.x 是一个高性能、异步非阻塞的 Java 网络框架（类似 Node.js）。
 * 它使用少量线程就能处理大量并发连接，非常适合构建 RPC 通信层。
 *
 * @Author HGL
 * @Create: 2025/8/29 15:12
 */
@Slf4j
public class VertxHttpServer implements HttpServer {

    @Override
    public void doStart(int port) {
        // 创建 Vert.x 实例（包含事件循环线程池）
        Vertx vertx = Vertx.vertx();

        // 创建 HTTP 服务器
        io.vertx.core.http.HttpServer httpServer = vertx.createHttpServer();

        // 设置请求处理器（所有请求交给 HttpServerHandler 处理）
        httpServer.requestHandler(new HttpServerHandler());

        // 启动服务器并监听端口
        httpServer.listen(port)
                .onSuccess(server -> log.info("HTTP server started on port {}", server.actualPort()))
                .onFailure(throwable -> log.error("Failed to start HTTP server: {}", throwable.getMessage()));
    }
}
