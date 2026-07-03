package com.hgl.hglrpc.protocol;

/**
 * 协议类型常量 —— "通信方式的选择"
 *
 * <p>RPC 框架支持两种底层通信协议：
 *   - TCP：自定义二进制协议，性能更高，适合内部服务间高频调用
 *   - HTTP：基于 HTTP 协议，更通用，适合对外暴露或跨语言调用
 *
 * <p>在 RpcConfig 中通过 protocol 字段指定使用哪种协议。
 * 框架会根据这个值，通过 SPI 加载对应的 Server 和 Client 实现。
 *
 * <pre>
 *   protocol="tcp"  → VertxTcpServer + VertxTcpClient（二进制协议，高性能）
 *   protocol="http" → VertxHttpServer + VertxHttpClient（HTTP 协议，通用性好）
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/9/8 16:53
 */
public interface ProtocolKeys {
    /** TCP 协议 —— 使用自定义二进制帧格式，性能优先 */
    String TCP = "tcp";
    /** HTTP 协议 —— 使用标准 HTTP 协议，兼容性优先 */
    String HTTP = "http";
}
