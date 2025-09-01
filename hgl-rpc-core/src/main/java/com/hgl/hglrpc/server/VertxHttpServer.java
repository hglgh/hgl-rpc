package com.hgl.hglrpc.server;

import io.vertx.core.Vertx;

/**
 * @ClassName: VertxHttpServer
 * @Package: com.hgl.hglrpc.server
 * @Description:
 * @Author HGL
 * @Create: 2025/8/29 15:12
 */
public class VertxHttpServer implements HttpServer {
    @Override
    public void doStart(int port) {

        // 创建vertx 实例
        Vertx vertx = Vertx.vertx();

        // 创建 HTTP 服务器
        io.vertx.core.http.HttpServer httpServer = vertx.createHttpServer();

        // 监听端口并处理请求
        httpServer.requestHandler(new HttpServerHandler());

        // 启动 HTTP 服务器并监听指定端口
        httpServer.listen(port)
                .onSuccess(server -> System.out.printf("HTTP server started on port %d\n", server.actualPort()))
                .onFailure(throwable -> System.out.println("Failed to start HTTP server: " + throwable.getMessage()));
    }
}
