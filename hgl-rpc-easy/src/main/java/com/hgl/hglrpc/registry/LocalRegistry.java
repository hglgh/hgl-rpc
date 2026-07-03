package com.hgl.hglrpc.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地注册中心 —— "服务的速查表"（easy 模块简化版）
 *
 * <p>这是一个极简的服务注册中心，用一个全局的 ConcurrentHashMap 存储
 * 服务名 → 服务实例的映射。服务提供者启动时注册，收到请求时查找。
 *
 * <p>注意：这个版本存的是实例（Object），不是 Class。
 * 这样避免了每次请求都 newInstance() 的开销。
 *
 * @Author HGL
 * @Create: 2025/8/29 15:26
 */
public class LocalRegistry {

    /** 服务名 → 服务实例的映射（线程安全的 HashMap） */
    private static final Map<String, Object> REGISTRY_MAP = new ConcurrentHashMap<>();

    /**
     * 注册服务 —— "在速查表中添加一条记录"
     *
     * @param serviceName     服务名（如 "UserService"）
     * @param serviceInstance 服务实例（如 UserServiceImpl 对象）
     */
    public static void register(String serviceName, Object serviceInstance) {
        REGISTRY_MAP.put(serviceName, serviceInstance);
    }

    /**
     * 获取服务 —— "根据名字查找实例"
     *
     * @param serviceName 服务名
     * @return 服务实例
     */
    public static Object get(String serviceName) {
        return REGISTRY_MAP.get(serviceName);
    }

    /**
     * 删除服务 —— "从速查表中移除"
     *
     * @param serviceName 服务名
     */
    public static void remove(String serviceName) {
        REGISTRY_MAP.remove(serviceName);
    }
}
