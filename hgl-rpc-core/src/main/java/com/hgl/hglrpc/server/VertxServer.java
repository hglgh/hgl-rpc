package com.hgl.hglrpc.server;

/**
 * 服务器的工作规范 —— "快递网点的岗位职责"
 *
 * <p>VertxServer 是所有服务器实现的顶层接口，就像快递网点的《岗位职责手册》——
 * 不管你是"顺丰网点"（TCP）还是"中通网点"（HTTP），都必须遵守同一个基本职责：
 * <b>开门营业，监听端口，等待客户的包裹（请求）上门。</b>
 *
 * <p>为什么需要这个接口？
 * <pre>
 *   ┌──────────────────────────────────────────────────────────────┐
 *   │                      VertxServer 接口                         │
 *   │              （快递网点的统一岗位职责）                           │
 *   │                                                              │
 *   │    +doStart(port): void    ← 所有网点都要"开门营业"              │
 *   │                                                              │
 *   │         ┌─────────────────────┬─────────────────────┐        │
 *   │         ▼                     ▼                     │        │
 *   │  ┌──────────────┐    ┌──────────────┐               │        │
 *   │  │VertxTcpServer│    │VertxHttpServer│    ...       │        │
 *   │  │（TCP 快递网点）│    │（HTTP 快递网点）│               │        │
 *   │  └──────────────┘    └──────────────┘               │        │
 *   └──────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>通过面向接口编程，上层调用方不需要关心具体是 TCP 还是 HTTP 服务器，
 * 只需通过 {@link VertxServerFactory} 拿到一个 VertxServer 实例，
 * 调用 doStart(port) 即可启动。这就像总公司只需要说"给我开一个网点"，
 * 不用管底下是哪个快递品牌在运营。
 *
 * @Author HGL
 * @Create: 2025/8/29 15:11
 * @see VertxServerFactory 服务器工厂，通过 SPI 机制按名称获取具体实现
 * @see com.hgl.hglrpc.server.tcp.VertxTcpServer TCP 快递网点
 * @see com.hgl.hglrpc.server.http.VertxHttpServer HTTP 快递网点
 */
public interface VertxServer {

    /**
     * 启动服务器 —— "开门营业"
     *
     * <p>在指定端口上启动服务器，开始监听客户端连接。
     * 调用后，服务器就像快递网点一样"开门"，等待"包裹"（网络请求）送上门。
     *
     * @param port 监听的端口号，就像快递网点的"门牌号"，客户端通过这个号码找到你
     */
    void doStart(int port);
}
