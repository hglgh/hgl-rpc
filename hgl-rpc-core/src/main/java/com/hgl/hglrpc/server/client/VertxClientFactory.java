package com.hgl.hglrpc.server.client;

import com.hgl.hglrpc.spi.SpiLoader;

/**
 * 客户端工厂 —— "快递员的调度中心"
 *
 * <p>VertxClientFactory 负责根据"快递品牌"（key）派发对应的快递员实例。
 * 和 {@link com.hgl.hglrpc.server.VertxServerFactory} 一样，
 * 它通过 {@link SpiLoader}（SPI 机制）加载具体实现，实现了调用方和实现方的解耦。
 *
 * <p>典型调用场景：
 * <pre>
 *   // 消费者发起 RPC 调用时，先从工厂获取一个"快递员"
 *   VertxClient client = VertxClientFactory.getInstance("tcp");
 *
 *   // 然后让快递员去送"包裹"
 *   RpcResponse response = client.doRequest(rpcRequest, serviceMetaInfo);
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/9/4 15:31
 * @see VertxClient 客户端接口（快递员的岗位职责）
 * @see SpiLoader SPI 加载器
 */
public class VertxClientFactory {

    /**
     * 获取客户端实例 —— "调度中心派发一名快递员"
     *
     * <p>根据 key（如 "tcp"、"http"）从 SPI 配置中找到对应的 VertxClient 实现类并创建实例。
     *
     * @param key 客户端类型标识，对应 SPI 配置文件中的 key，例如 "tcp" 对应 VertxTcpClient
     * @return 具体的客户端实例（快递员）
     */
    public static VertxClient getInstance(String key) {
        return SpiLoader.getInstance(VertxClient.class, key);
    }
}
