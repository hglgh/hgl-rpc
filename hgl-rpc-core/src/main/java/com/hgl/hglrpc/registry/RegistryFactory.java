package com.hgl.hglrpc.registry;

import com.hgl.hglrpc.spi.SpiLoader;

/**
 * @ClassName: RegistryFactory
 * @Package: com.hgl.hglrpc.registry
 * @Description: 注册中心工厂（用于获取注册中心对象）
 * @Author HGL
 * @Create: 2025/9/2 14:05
 */
public class RegistryFactory {

    /**
     * 默认注册中心
     */
    private static final Registry DEFAULT_REGISTRY = new EtcdRegistry();

    /**
     * 是否已加载SPI
     */
    private static volatile boolean loaded = false;

    /**
     * 获取实例
     *
     * @param key 键
     * @return 实例
     */
    public static Registry getInstance(String key) {
        // 双重检查锁定实现懒加载
        if (!loaded) {
            synchronized (RegistryFactory.class) {
                if (!loaded) {
                    SpiLoader.load(Registry.class);
                    loaded = true;
                }
            }
        }
        return SpiLoader.getInstance(Registry.class, key);
    }
}
