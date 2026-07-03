package com.hgl.hglrpc.registry;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import com.hgl.hglrpc.config.RegistryConfig;
import com.hgl.hglrpc.model.ServiceMetaInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.CuratorCache;
import org.apache.curator.framework.recipes.cache.CuratorCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.curator.x.discovery.ServiceDiscovery;
import org.apache.curator.x.discovery.ServiceDiscoveryBuilder;
import org.apache.curator.x.discovery.ServiceInstance;
import org.apache.curator.x.discovery.details.JsonInstanceSerializer;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * ZooKeeper 注册中心实现 —— "基于树形结构的电话簿"
 *
 * <p>ZooKeeper 是 Apache 的分布式协调服务，内部是一棵"文件树"（ZNode Tree）。
 * 它用"临时节点"（Ephemeral Node）实现服务注册：
 * 服务提供者创建临时节点，连接断开后节点自动删除——无需手动续期。
 *
 * <p>与 Etcd 的对比：
 * <pre>
 *   特性            Etcd                    ZooKeeper
 *   ────────────    ──────────────────      ──────────────────
 *   数据结构        KV 存储                  树形 ZNode
 *   心跳机制        Lease + keepAlive       临时节点（自动删除）
 *   监听机制        Watch                   CuratorCache
 *   适用场景        K8s、云原生              Hadoop 生态
 *   客户端          jetcd                   Apache Curator
 * </pre>
 *
 * <p>使用 Apache Curator 封装 ZooKeeper 操作（比原生 ZK API 更易用）。
 * Curator 的 ServiceDiscovery 提供了开箱即用的服务注册/发现能力。
 *
 * @Author HGL
 * @Create: 2025/9/3 11:10
 */
@Slf4j
public class ZooKeeperRegistry implements Registry {

    /** ZooKeeper 中的根路径 */
    private static final String ZK_ROOT_PATH = "/rpc/zk";

    /** Curator 客户端 */
    private CuratorFramework client;

    /** 服务发现客户端（Curator 封装的高级 API） */
    private ServiceDiscovery<ServiceMetaInfo> serviceDiscovery;

    /** 本机注册的节点 key 集合 */
    private final Set<String> localRegisterNodeKeySet = new HashSet<>();

    /** 服务发现结果缓存 */
    private final RegistryServiceMultiCache registryServiceMultiCache = new RegistryServiceMultiCache();

    /** 正在监听的 key 集合（去重） */
    private final Set<String> watchingKeySet = new ConcurrentHashSet<>();

    @Override
    public void init(RegistryConfig registryConfig) {
        // 创建 Curator 客户端（带指数退避重试策略）
        client = CuratorFrameworkFactory.builder()
                .connectString(registryConfig.getAddress())
                .retryPolicy(new ExponentialBackoffRetry(registryConfig.getTimeout().intValue(), 3))
                .build();

        // 创建 ServiceDiscovery（Curator 的服务发现工具）
        serviceDiscovery = ServiceDiscoveryBuilder.builder(ServiceMetaInfo.class)
                .client(client)
                .basePath(ZK_ROOT_PATH)
                .serializer(new JsonInstanceSerializer<>(ServiceMetaInfo.class))
                .build();

        try {
            client.start();
            serviceDiscovery.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void register(ServiceMetaInfo serviceMetaInfo) throws Exception {
        // 注册服务实例到 ZK（创建临时节点）
        serviceDiscovery.registerService(buildServiceInstance(serviceMetaInfo));
        // 记录到本地缓存
        String registerKey = String.format("%s/%s", ZK_ROOT_PATH, serviceMetaInfo.getServiceNodeKey());
        localRegisterNodeKeySet.add(registerKey);
    }

    @Override
    public void unRegister(ServiceMetaInfo serviceMetaInfo) {
        try {
            // 从 ZK 注销服务实例
            serviceDiscovery.unregisterService(buildServiceInstance(serviceMetaInfo));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String registerKey = String.format("%s/%s", ZK_ROOT_PATH, serviceMetaInfo.getServiceNodeKey());
        localRegisterNodeKeySet.remove(registerKey);
    }

    @Override
    public List<ServiceMetaInfo> serviceDiscovery(String serviceKey) {
        // 优先从缓存获取
        List<ServiceMetaInfo> serviceMetaInfoList = registryServiceMultiCache.readCache(serviceKey);
        if (CollUtil.isNotEmpty(serviceMetaInfoList)) {
            return serviceMetaInfoList;
        }
        try {
            // 从 ZK 查询服务实例列表
            Collection<ServiceInstance<ServiceMetaInfo>> serviceInstanceList = serviceDiscovery.queryForInstances(serviceKey);
            serviceMetaInfoList = serviceInstanceList.stream()
                    .map(ServiceInstance::getPayload)
                    .collect(Collectors.toList());
            // 写入缓存
            serviceMetaInfoList.forEach(serviceMetaInfo ->
                    registryServiceMultiCache.writeCache(serviceKey, serviceMetaInfo.getServiceNodeKey(), serviceMetaInfo));
            return serviceMetaInfoList;
        } catch (Exception e) {
            throw new RuntimeException("获取服务列表失败", e);
        }
    }

    @Override
    public void heartBeat() {
        // ZooKeeper 使用临时节点，连接断开自动删除，无需手动心跳续期
    }

    @Override
    public void watch(String serviceKey, String serviceNodeKey) {
        String watchKey = String.format("%s/%s", ZK_ROOT_PATH, serviceNodeKey);
        boolean newWatch = watchingKeySet.add(watchKey);
        if (newWatch) {
            // 使用 CuratorCache 监听节点变化
            CuratorCache curatorCache = CuratorCache.build(client, watchKey);
            curatorCache.start();
            curatorCache.listenable().addListener(CuratorCacheListener.builder()
                    // 节点被删除时清除缓存
                    .forDeletes(childData -> registryServiceMultiCache.clearCache(serviceKey, serviceNodeKey))
                    // 节点内容变化时也清除缓存
                    .forChanges((oldData, newData) -> registryServiceMultiCache.clearCache(serviceKey, serviceNodeKey))
                    .build());
        }
    }

    @Override
    public void destroy() {
        log.info("当前节点下线");
        // 删除本节点（临时节点在连接关闭后会自动删除，这里主动删除更优雅）
        for (String key : localRegisterNodeKeySet) {
            try {
                client.delete().guaranteed().forPath(key);
            } catch (Exception e) {
                throw new RuntimeException(key + "下线失败", e);
            }
        }
        localRegisterNodeKeySet.clear();
        if (client != null) {
            client.close();
        }
    }

    /**
     * 构建 ZK ServiceInstance 对象
     *
     * @param serviceMetaInfo 服务元信息
     * @return Curator 的 ServiceInstance 对象
     */
    private ServiceInstance<ServiceMetaInfo> buildServiceInstance(ServiceMetaInfo serviceMetaInfo) {
        String serviceAddress = String.format("%s:%s", serviceMetaInfo.getServiceHost(), serviceMetaInfo.getServicePort());
        try {
            return ServiceInstance.<ServiceMetaInfo>builder()
                    .id(serviceAddress)
                    .name(serviceMetaInfo.getServiceKey())
                    .address(serviceAddress)
                    .payload(serviceMetaInfo)
                    .build();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
