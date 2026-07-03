package com.hgl.hglrpc.serializer;

import com.hgl.hglrpc.spi.SpiLoader;

/**
 * 序列化器工厂 —— "翻译官调度中心"
 *
 * <p>当框架需要序列化/反序列化时，不直接 new 具体的序列化器，
 * 而是通过这个工厂来获取。工厂内部通过 SPI 机制加载具体实现。
 *
 * <p>使用方式：
 * <pre>
 *   // 根据配置文件中的名称获取序列化器
 *   Serializer serializer = SerializerFactory.getInstance("kryo");
 *   byte[] bytes = serializer.serialize(rpcRequest);
 * </pre>
 *
 * <p>这种工厂模式的好处：
 * <pre>
 *   1. 调用方不依赖具体实现类（面向接口编程）
 *   2. 可以通过配置文件切换序列化方式，无需改代码
 *   3. 序列化器实例是单例的（由 SpiLoader 缓存），避免重复创建
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/9/1 16:51
 */
public class SerializerFactory {

    /**
     * 获取序列化器实例
     *
     * @param key 序列化器名称（如 "jdk", "kryo", "hessian", "json"）
     * @return 序列化器单例实例
     */
    public static Serializer getInstance(String key) {
        return SpiLoader.getInstance(Serializer.class, key);
    }
}
