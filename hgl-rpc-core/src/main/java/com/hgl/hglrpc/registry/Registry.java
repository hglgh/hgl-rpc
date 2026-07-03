package com.hgl.hglrpc.registry;

import com.hgl.hglrpc.config.RegistryConfig;
import com.hgl.hglrpc.model.ServiceMetaInfo;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * 注册中心接口 —— "电话簿的工作规范"
 *
 * <p>在 RPC 框架中，注册中心扮演"电话簿"的角色：
 * <pre>
 *   服务提供者（Provider）：在电话簿上登记自己的号码 → register()
 *   服务消费者（Consumer）：从电话簿上查找对方号码 → serviceDiscovery()
 *   服务下线时：从电话簿上删除自己的号码 → unRegister()
 *   定期续费：防止号码被自动回收 → heartBeat()
 *   变更通知：有人登记/删除时通知查询过的人 → watch()
 * </pre>
 *
 * <p>本框架支持两种注册中心实现：
 * <pre>
 *   EtcdRegistry      —— 基于 Etcd（分布式 KV 存储，类似简化版 ZooKeeper）
 *   ZooKeeperRegistry —— 基于 ZooKeeper（老牌分布式协调服务）
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/9/2 13:50
 */
public interface Registry {

    /**
     * 初始化 —— "打开电话簿"
     *
     * <p>连接到注册中心服务器（如 Etcd、ZooKeeper）。
     * 必须在使用其他方法前调用。
     *
     * @param registryConfig 注册中心配置（地址、超时等）
     */
    void init(RegistryConfig registryConfig);

    /**
     * 注册服务 —— "在电话簿上登记号码"
     *
     * <p>服务提供者启动时调用，将自己的地址信息写入注册中心。
     * 消费者随后就能通过 serviceDiscovery() 找到这个节点。
     *
     * @param serviceMetaInfo 服务元信息（包含服务名、地址、端口等）
     */
    void register(ServiceMetaInfo serviceMetaInfo) throws Exception;

    /**
     * 注销服务 —— "从电话簿上删除号码"
     *
     * <p>服务提供者下线时调用，避免消费者继续调用已下线的节点。
     *
     * @param serviceMetaInfo 要注销的服务元信息
     */
    void unRegister(ServiceMetaInfo serviceMetaInfo) throws ExecutionException, InterruptedException;

    /**
     * 服务发现 —— "在电话簿上查找号码"
     *
     * <p>消费者调用，根据服务名查找所有可用的服务提供者节点。
     * 返回一个列表，消费者通过负载均衡从中选择一个节点进行调用。
     *
     * @param serviceKey 服务键名（如 "com.hgl.example.common.service.UserService:1.0"）
     * @return 该服务的所有可用节点列表
     */
    List<ServiceMetaInfo> serviceDiscovery(String serviceKey);

    /**
     * 心跳检测 —— "定期给电话簿续费"
     *
     * <p>注册中心通常用租约（Lease/TTL）机制来自动清理死节点。
     * 如果服务提供者不续期，租约到期后节点信息会被自动删除。
     * 这个方法启动一个定时任务，周期性地续期所有已注册的节点。
     */
    void heartBeat();

    /**
     * 监听变更 —— "订阅电话簿的更新通知"
     *
     * <p>消费者调用，注册对某个服务的监听。
     * 当该服务的节点发生变化（新增/删除）时，注册中心会推送通知，
     * 框架据此更新本地缓存。
     *
     * @param serviceKey     服务键名
     * @param serviceNodeKey 服务节点键名
     */
    void watch(String serviceKey, String serviceNodeKey);

    /**
     * 销毁 —— "关闭电话簿"
     *
     * <p>JVM 退出时调用，注销所有本节点注册的服务，释放连接资源。
     * 通过 ShutdownHook 自动触发，无需手动调用。
     */
    void destroy();
}
