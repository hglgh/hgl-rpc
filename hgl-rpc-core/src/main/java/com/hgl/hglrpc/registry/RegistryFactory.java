package com.hgl.hglrpc.registry;

import com.hgl.hglrpc.spi.SpiLoader;

/**
 * 注册中心工厂 —— "电话簿的选购窗口"
 *
 * <p>消费者和提供者通过这个工厂获取注册中心实例。
 * 内部通过 SPI 机制（{@link SpiLoader}）根据配置文件中的 key
 * 加载对应的 Registry 实现（Etcd、ZooKeeper 等）。
 *
 * <p>调用方式：
 * <pre>
 *   Registry registry = RegistryFactory.getInstance("etcd");
 *   registry.init(config);
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/9/2 14:05
 */
public class RegistryFactory {

    /**
     * 根据 key 获取注册中心实例
     *
     * @param key 注册中心类型标识（如 "etcd"、"zookeeper"）
     * @return 对应的 Registry 实现
     */
    public static Registry getInstance(String key) {
        return SpiLoader.getInstance(Registry.class, key);
    }
}
