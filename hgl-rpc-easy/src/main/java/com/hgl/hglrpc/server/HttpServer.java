package com.hgl.hglrpc.server;

/**
 * HTTP 服务器接口 —— "快递网点的岗位职责"
 *
 * <p>这是 hgl-rpc-easy 模块的简化版服务器接口。
 * 只需要一个方法：启动服务器并监听指定端口。
 *
 * <p>这是整个 RPC 框架最早期的原型，功能精简但五脏俱全，
 * 适合理解 RPC 的核心原理。正式版在 hgl-rpc-core 模块中。
 *
 * @Author HGL
 * @Create: 2025/8/29 15:11
 */
public interface HttpServer {

    /**
     * 启动 HTTP 服务器 —— "开门营业"
     *
     * @param port 监听的端口号
     */
    void doStart(int port);
}
