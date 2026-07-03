package com.hgl.hglrpc.server;

import com.hgl.hglrpc.spi.SpiLoader;

/**
 * 服务器工厂 —— "快递网点的总部派发中心"
 *
 * <p>VertxServerFactory 负责根据"品牌名"（key）生产对应的服务器实例。
 * 它不自己造服务器，而是委托给 {@link SpiLoader}（SPI 机制），
 * 从配置文件中读取"品牌名 → 实现类"的映射关系，然后反射创建实例。
 *
 * <p>为什么用工厂 + SPI 而不是 new？
 * <pre>
 *   ╔═══════════════════════════════════════════════════════════════════╗
 *   ║  传统写法（硬编码）：                                             ║
 *   ║    VertxServer server = new VertxTcpServer();  ← 耦合！          ║
 *   ║    想换成 HTTP？得改代码、重新编译。                               ║
 *   ║                                                                   ║
 *   ║  工厂 + SPI 写法：                                                ║
 *   ║    VertxServer server = VertxServerFactory.getInstance("tcp");    ║
 *   ║    想换成 HTTP？改配置文件就行，代码一行不动。                      ║
 *   ╚═══════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <p>这就像是：
 * - 硬编码 = 你亲自去"顺丰网点"寄快递，换快递公司就得换网点
 * - 工厂+SPI = 你打客服电话说"我要寄快递"，总部自动派发给对应的网点
 *
 * @Author HGL
 * @Create: 2025/9/4 15:14
 * @see VertxServer 服务器接口
 * @see SpiLoader SPI 加载器，负责从配置文件中加载具体实现类
 */
public class VertxServerFactory {

    /**
     * 获取服务器实例 —— "总部派发一个网点"
     *
     * <p>根据 key（如 "tcp"、"http"）从 SPI 配置中找到对应的 VertxServer 实现类并创建实例。
     *
     * @param key 服务器类型标识，对应 SPI 配置文件中的 key，例如 "tcp" 对应 VertxTcpServer
     * @return 具体的服务器实例
     */
    public static VertxServer getInstance(String key) {
        return SpiLoader.getInstance(VertxServer.class, key);
    }
}
