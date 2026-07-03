package com.hgl.hglrpc;

import com.hgl.hglrpc.config.RegistryConfig;
import com.hgl.hglrpc.config.RpcConfig;
import com.hgl.hglrpc.constant.RpcConstant;
import com.hgl.hglrpc.registry.Registry;
import com.hgl.hglrpc.registry.RegistryFactory;
import com.hgl.hglrpc.utils.ConfigUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * RPC 框架应用入口 —— "整个框架的心脏"
 *
 * <p>这个类是 RPC 框架的全局单例持有者，负责：
 * <pre>
 *   1. 加载配置（从 application.yml 读取 RpcConfig）
 *   2. 初始化注册中心（连接 Etcd / ZooKeeper）
 *   3. 启动心跳续期（provider 角色）
 *   4. 注册 JVM Shutdown Hook（优雅关闭）
 *   5. 提供全局配置访问（getRpcConfig()）
 * </pre>
 *
 * <p>使用双重检查锁（DCL）实现懒加载单例：
 * <pre>
 *   第一次调用 getRpcConfig() 时才初始化，之后直接返回缓存。
 *   volatile + synchronized + 双重 null 检查 = 线程安全的懒加载。
 * </pre>
 *
 * <p>初始化流程：
 * <pre>
 *   getRpcConfig()
 *       │
 *       ├── rpcConfig == null? ──→ 从 application.yml 加载配置
 *       │                              │
 *       │                              ▼
 *       │                        init(rpcConfig)
 *       │                              │
 *       │                    ┌─────────┴─────────┐
 *       │                    ▼                   ▼
 *       │            初始化注册中心          注册 Shutdown Hook
 *       │            (Etcd/ZK)            (JVM退出时注销服务)
 *       │                    │
 *       │                    ▼
 *       │            启动心跳续期（仅 provider）
 *       │
 *       ▼
 *   返回 rpcConfig（后续调用直接返回缓存）
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/9/1 10:47
 */
@Slf4j
public class RpcApplication {

    /**
     * 全局 RPC 配置（volatile 保证多线程可见性）
     *
     * <p>volatile 的作用：当一个线程初始化了 rpcConfig 后，
     * 其他线程能立即看到这个变化，不会读到旧值（null）。
     */
    private static volatile RpcConfig rpcConfig;

    /**
     * 内部初始化方法 —— 完成所有初始化工作
     *
     * @param newRpcConfig 配置对象
     */
    private static void init(RpcConfig newRpcConfig) {
        rpcConfig = newRpcConfig;
        log.info("rpc init, config = {}", newRpcConfig.toString());

        // 初始化注册中心（连接 Etcd 或 ZooKeeper）
        RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
        Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
        registry.init(registryConfig);
        log.info("registry init, config = {}", registryConfig);

        // 仅 provider 角色需要：启动心跳续期 + 注册关闭钩子
        if ("provider".equalsIgnoreCase(rpcConfig.getRole())) {
            // 启动定时心跳，防止注册中心中的节点信息过期
            registry.heartBeat();
            // 注册 Shutdown Hook：JVM 退出时自动注销服务，避免消费者调到已下线的节点
            Runtime.getRuntime().addShutdownHook(new Thread(registry::destroy));
        }
    }

    /**
     * 公开的初始化方法（从配置文件加载）
     */
    public static void init() {
        RpcConfig newRpcConfig;
        try {
            newRpcConfig = ConfigUtils.loadConfig(RpcConfig.class, RpcConstant.DEFAULT_CONFIG_PREFIX);
        } catch (Exception e) {
            // 配置加载失败时使用全默认值，框架仍可运行
            newRpcConfig = new RpcConfig();
            log.error("load config error, use default config");
        }
        init(newRpcConfig);
    }

    /**
     * 获取全局 RPC 配置 —— "读取控制面板"
     *
     * <p>使用双重检查锁（DCL）实现懒加载：
     * <pre>
     *   if (rpcConfig == null) {          // 第一次检查（无锁，快速路径）
     *       synchronized (RpcApplication.class) {
     *           if (rpcConfig == null) {  // 第二次检查（有锁，防止重复初始化）
     *               init();
     *           }
     *       }
     *   }
     *   return rpcConfig;
     * </pre>
     *
     * @return 全局 RPC 配置对象
     */
    public static RpcConfig getRpcConfig() {
        if (rpcConfig == null) {
            synchronized (RpcApplication.class) {
                if (rpcConfig == null) {
                    init();
                }
            }
        }
        return rpcConfig;
    }
}
