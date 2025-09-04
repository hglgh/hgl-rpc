package com.hgl.hglrpc.server;

/**
 * @ClassName: HttpServer
 * @Package: com.hgl.hglrpc.server
 * @Description: HTTP 服务器接口
 * @Author HGL
 * @Create: 2025/8/29 15:11
 */
public interface VertxServer {
    /**
     * 启动 HTTP 服务器
     *
     * @param port 端口
     */
    void doStart(int port);
}
