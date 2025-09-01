package com.hgl.hglrpc.serializer;

import com.hgl.hglrpc.spi.SpiLoader;

import java.util.HashMap;
import java.util.Map;

/**
 * @ClassName: SerializerFactory
 * @Package: com.hgl.hglrpc.serializer
 * @Description: 序列化器工厂（用于获取序列化器对象）
 * @Author HGL
 * @Create: 2025/9/1 16:51
 */
public class SerializerFactory {
    static {
        SpiLoader.load(Serializer.class);
    }

    /**
     * 默认序列化器
     */
    private static final Serializer DEFAULT_SERIALIZER = new JdkSerializer();

    /**
     * 获取实例
     *
     * @param key 键
     * @return 实例
     */
    public static Serializer getInstance(String key) {
        return SpiLoader.getInstance(Serializer.class, key);
    }
}
