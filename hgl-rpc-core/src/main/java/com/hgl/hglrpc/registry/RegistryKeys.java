package com.hgl.hglrpc.registry;

/**
 * 注册中心键名常量 —— "电话簿的种类编号"
 *
 * <p>配置文件中通过这些常量指定使用哪种注册中心。
 * 框架根据这个值通过 SPI 加载对应的 Registry 实现。
 *
 * @Author HGL
 * @Create: 2025/9/2 14:05
 */
public interface RegistryKeys {

    /** Etcd —— 轻量级分布式 KV 存储，K8s 的基石 */
    String ETCD = "etcd";

    /** ZooKeeper —— 老牌分布式协调服务，Hadoop 生态的标配 */
    String ZOOKEEPER = "zookeeper";
}
