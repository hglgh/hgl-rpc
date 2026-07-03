package com.hgl.hglrpc.config;

import lombok.Data;

/**
 * 注册中心配置 —— "电话簿的连接信息"
 *
 * <p>注册中心是 RPC 框架的"电话簿"——服务提供者在上面登记自己的地址，
 * 消费者从上面查找要调用的服务在哪里。
 * 这个类配置了"如何连接到这个电话簿"。
 *
 * <p>配置示例（application.yml）：
 * <pre>
 *   rpc:
 *     registryConfig:
 *       registry: etcd
 *       address: http://192.168.6.128:2379
 *       username: root
 *       password: secret
 *       timeout: 10000
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/9/2 13:48
 */
@Data
public class RegistryConfig {

    /**
     * 注册中心类型
     *
     * <p>支持："etcd"（默认）、"zookeeper"
     * 通过 SPI 机制加载对应的 Registry 实现类。
     */
    private String registry = "etcd";

    /**
     * 注册中心连接地址
     *
     * <p>Etcd 示例："<a href="http://192.168.6.128:2379">...</a>"
     * ZooKeeper 示例："192.168.6.128:2181"
     */
    private String address = "http://192.168.6.128:2379";

    /** 用户名（如果注册中心启用了认证） */
    private String username;

    /** 密码 */
    private String password;

    /**
     * 连接超时时间（毫秒）
     *
     * <p>如果在这个时间内连不上注册中心，会抛出异常。
     */
    private Long timeout = 10000L;
}
