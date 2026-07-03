package com.hgl.hglrpc.registry;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 本地注册表 —— "服务的速查表"
 *
 * <p>LocalRegistry 是服务提供者端的"速查表"，存储了本机对外提供的所有服务实例。
 * 当收到 RPC 请求时，服务端从这里查找对应的服务实例，然后通过反射调用目标方法。
 *
 * <p>它和"远程注册中心"（Etcd/ZooKeeper）的区别：
 * <pre>
 *   LocalRegistry（本地）          远程注册中心（Etcd/ZK）
 *   ─────────────────────        ─────────────────────────
 *   存储：服务名 → 实例对象        存储：服务名 → 网络地址列表
 *   用途：执行方法调用              用途：服务发现和负载均衡
 *   位置：每个 JVM 进程内           位置：独立的分布式集群
 *   格式：Object（活的对象）        格式：JSON 字符串
 * </pre>
 *
 * <p>为什么存储实例（Object）而不是类（Class）？
 * <pre>
 *   旧方案（存Class）：每次请求都 newInstance() 创建新对象
 *   新方案（存Object）：启动时创建一次，之后直接复用
 *
 *   好处：
 *   1. 性能提升：避免每请求都反射创建对象 + GC
 *   2. Spring 兼容：存储的是 Spring 管理的 Bean，支持依赖注入
 *   3. 状态支持：服务实现类可以持有状态（如计数器、缓存等）
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/8/29 15:26
 */
public class LocalRegistry {

    /**
     * 服务注册表：服务名 → 服务实例
     *
     * <p>使用 ConcurrentHashMap 保证线程安全（多个请求线程并发查找）。
     * key = 服务接口的全限定类名
     * value = 服务实现类的实例对象
     */
    private static final Map<String, Object> REGISTRY_MAP = new ConcurrentHashMap<>();

    /**
     * 注册服务 —— "在速查表中登记一个服务"
     *
     * <p>通常在框架启动时调用（ProviderBootstrap 或 Spring 的 BeanPostProcessor）。
     * 注册后，收到 RPC 请求时就能根据 serviceName 找到对应的实例来调用。
     *
     * @param serviceName     服务名（接口的全限定类名）
     * @param serviceInstance 服务实例（实现类的对象）
     */
    public static void register(String serviceName, Object serviceInstance) {
        REGISTRY_MAP.put(serviceName, serviceInstance);
    }

    /**
     * 获取服务实例 —— "根据名字查速查表"
     *
     * @param serviceName 服务名
     * @return 服务实例，找不到返回 null
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
