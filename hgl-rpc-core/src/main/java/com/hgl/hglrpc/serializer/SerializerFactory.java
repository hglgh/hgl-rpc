package com.hgl.hglrpc.serializer;

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
    /**
     * 序列化映射（用于实现单例）
     */
    private static final Map<String, Serializer> KEY_SERIALIZER_MAP = new HashMap<String, Serializer>() {{
        put(SerializerKeys.JDK, new JdkSerializer());
        put(SerializerKeys.JSON, new JsonSerializer());
        put(SerializerKeys.KRYO, new KryoSerializer());
        put(SerializerKeys.HESSIAN, new HessianSerializer());
    }};

    /**
     * 默认序列化器
     */
    private static final Serializer DEFAULT_SERIALIZER = KEY_SERIALIZER_MAP.get("jdk");

    /**
     * 获取实例
     *
     * @param key 键
     * @return 实例
     */
    public static Serializer getInstance(String key) {
        //尝试根据指定key获取序列化器，如果找不到则返回默认序列化器(DEFAULT_SERIALIZER)
        return KEY_SERIALIZER_MAP.getOrDefault(key, DEFAULT_SERIALIZER);
    }
}
