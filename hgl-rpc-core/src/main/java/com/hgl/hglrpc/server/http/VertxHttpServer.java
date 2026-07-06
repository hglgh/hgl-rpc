package com.hgl.hglrpc.server.http;

import com.hgl.hglrpc.server.VertxServer;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServer;
import lombok.extern.slf4j.Slf4j;

/**
 * HTTP 快递网点 —— 基于 Vert.x 的 HTTP 服务器实现
 *
 * <p>VertxHttpServer 是 {@link VertxServer} 接口的 HTTP 实现，
 * 它启动一个基于 Vert.x 的 HTTP 服务器，通过 HTTP POST 请求接收 RPC 调用。
 *
 * <p>与 {@link com.hgl.hglrpc.server.tcp.VertxTcpServer} 的区别：
 * <pre>
 *   ╔═══════════════════════════════════════════════════════════════════╗
 *   ║  对比维度          │  TCP 快递网点            │  HTTP 快递网点         ║
 *   ╠═══════════════════════════════════════════════════════════════════╣
 *   ║  协议层            │  TCP（传输层）           │  HTTP（应用层）        ║
 *   ║  数据格式          │  自定义二进制协议         │  HTTP Body            ║
 *   ║  粘包拆包          │  需要手动处理            │  HTTP 自带边界          ║
 *   ║  性能             │  更高（无协议开销）       │  稍低（HTTP 头）        ║
 *   ║  可调试性          │  需要专用工具            │  curl/浏览器即可        ║
 *   ║  适用场景          │  内网高性能通信          │  跨网/调试/兼容          ║
 *   ╚═══════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <p>HTTP 版本省去了粘包拆包和自定义协议编解码，因为 HTTP 协议本身
 * 就通过 Content-Length / Transfer-Encoding 解决了消息边界问题。
 * 请求体和响应体直接使用序列化器进行序列化/反序列化。
 *
 * @Author HGL
 * @Create: 2025/8/29 15:12
 * @see HttpServerHandler HTTP 快递员，负责具体的请求处理逻辑
 * @see com.hgl.hglrpc.server.tcp.VertxTcpServer TCP 快递网点（对比参考）
 */
@Slf4j
public class VertxHttpServer implements VertxServer {

    /**
     * 启动 HTTP 服务器 —— "HTTP 快递网点开门营业"
     *
     * <p>创建 Vert.x 实例和 HTTP 服务器，绑定端口开始监听。
     * 与 TCP 版本不同，HTTP 版本使用 {@code createHttpServer()} 而不是 {@code createNetServer()}，
     * 因为 HTTP 是应用层协议，Vert.x 会自动处理 HTTP 请求的解析（请求行、请求头、请求体）。
     *
     * @param port 监听的端口号
     */
    @Override
    public void doStart(int port) {
        // 创建 Vert.x 实例 —— 租场地、招人
        Vertx vertx = Vertx.vertx();

        // 创建 HTTP 服务器 —— 挂上"HTTP 快递网点"的招牌
        // 注意：这里用 createHttpServer() 而非 createNetServer()
        // HTTP 服务器会自动解析 HTTP 协议，不需要手动处理粘包拆包
        HttpServer httpServer = vertx.createHttpServer();

        // 注册请求处理器 —— 安排 HTTP 快递员（HttpServerHandler）上岗
        httpServer.requestHandler(new HttpServerHandler());

        // 启动 HTTP 服务器并监听指定端口 —— 开门营业！
        httpServer.listen(port)
                .onSuccess(server -> log.info("HTTP server started on port {}", server.actualPort()))
                .onFailure(throwable -> log.error("Failed to start HTTP server: {}", throwable.getMessage()));
    }
}
