package com.hgl.hglrpc.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName: LocalRegistry
 * @Package: com.hgl.hglrpc.registry
 * @Description: 本地注册中心
 * @Author HGL
 * @Create: 2025/8/29 15:26
 */
public class LocalRegistry {

    /**
     * 注册信息存储
     */
    private static final Map<String, Class<?>> REGISTRY_MAP = new ConcurrentHashMap<>();

    /**
     * 注册服务
     *
     * @param serviceName 服务名
     * @param implClass   实现类
     */
    public static void register(String serviceName, Class<?> implClass) {
        REGISTRY_MAP.put(serviceName, implClass);
    }

    /**
     * 获取服务
     *
     * @param serviceName 服务名
     * @return 服务
     */
    public static Class<?> get(String serviceName) {
        return REGISTRY_MAP.get(serviceName);
    }

    /**
     * 删除服务
     *
     * @param serviceName 服务名
     */
    public static void remove(String serviceName) {
        REGISTRY_MAP.remove(serviceName);
    }
}
