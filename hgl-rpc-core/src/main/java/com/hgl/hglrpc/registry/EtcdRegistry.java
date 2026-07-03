package com.hgl.hglrpc.registry;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ConcurrentHashSet;
import cn.hutool.cron.CronUtil;
import cn.hutool.cron.task.Task;
import cn.hutool.json.JSONUtil;
import com.hgl.hglrpc.config.RegistryConfig;
import com.hgl.hglrpc.model.ServiceMetaInfo;
import io.etcd.jetcd.*;
import io.etcd.jetcd.options.GetOption;
import io.etcd.jetcd.options.PutOption;
import io.etcd.jetcd.watch.WatchEvent;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

/**
 * Etcd 注册中心实现 —— "基于分布式 KV 存储的电话簿"
 *
 * <p>Etcd 是一个分布式、可靠的键值存储系统（Kubernetes 用它存储集群状态）。
 * 它天然支持"租约"（Lease）机制：给 key 设一个 TTL，到期自动删除，
 * 除非你定期"续期"（keepAlive）。这正好满足注册中心的需求：
 * 服务挂了不再续期 → 租约过期 → key 自动删除 → 消费者不再调用这个节点。
 *
 * <p>在 Etcd 中的存储结构：
 * <pre>
 *   /rpc/                                                  ← 根路径
 *   └── com.hgl.example.common.service.UserService:1.0     ← 服务键
 *       └── /192.168.1.100:8080                            ← 节点键
 *           → {"serviceName":"...","serviceHost":"192.168.1.100","servicePort":8080,...}
 *
 *   一个服务名下可以有多个节点（多个 IP:Port），消费者发现后通过负载均衡选择一个。
 * </pre>
 *
 * <p>核心机制：
 * <pre>
 *   register()    → 创建 Lease（30秒TTL）+ Put key（关联Lease）+ 缓存 leaseId
 *   heartBeat()   → 每 10 秒 keepAliveOnce(leaseId) 续期，防止过期
 *   destroy()     → Delete 所有 key + 关闭连接（ShutdownHook 触发）
 *   serviceDiscovery() → 前缀查询 + 本地缓存 + Watch 监听变更
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/9/2 11:19
 */
@Slf4j
public class EtcdRegistry implements Registry {

    /** Etcd 客户端连接 */
    private Client client;

    /** KV 操作客户端（用于读写键值对） */
    private KV kvClient;

    /**
     * 本机注册的节点 key 集合 —— "我登记了哪些号码"
     *
     * <p>续期和注销时需要遍历这个集合。
     * 只存本节点注册的 key，不包含从其他节点发现的 key。
     */
    private final Set<String> localRegisterNodeKeySet = new HashSet<>();

    /**
     * 租约 ID 缓存 —— "每个号码的续期凭证"
     *
     * <p>结构：registerKey → leaseId
     * 注册时创建 Lease 并缓存 leaseId，续期时直接用，
     * 避免每次续期都重新创建 Lease（性能优化）。
     */
    private final Map<String, Long> leaseIdCache = new ConcurrentHashMap<>();

    /**
     * 服务发现结果缓存 —— "查过的号码记在小本本上"
     *
     * <p>避免每次都去 Etcd 查询，减轻 Etcd 压力。
     * 当 Watch 监听到节点变更时，清除对应的缓存。
     */
    private final RegistryServiceMultiCache registryServiceMultiCache = new RegistryServiceMultiCache();

    /**
     * 正在监听的 key 集合 —— "已订阅更新通知的号码"
     *
     * <p>防止对同一个 key 重复注册 Watch（每个 Watch 会消耗 Etcd 资源）。
     */
    private final Set<String> watchingKeySet = new ConcurrentHashSet<>();

    /** Etcd 中存储服务信息的根路径 */
    private static final String ETCD_ROOT_PATH = "/rpc/";

    @Override
    public void init(RegistryConfig registryConfig) {
        // 创建 Etcd 客户端连接（带超时配置）
        client = Client.builder()
                .endpoints(registryConfig.getAddress())
                .connectTimeout(Duration.ofMillis(registryConfig.getTimeout()))
                .build();
        kvClient = client.getKVClient();
    }

    @Override
    public void register(ServiceMetaInfo serviceMetaInfo) throws Exception {
        // 1. 创建 Lease 客户端
        Lease leaseClient = client.getLeaseClient();

        // 2. 创建一个 30 秒的租约（30 秒内不续期，key 会被自动删除）
        long leaseId = leaseClient.grant(30).get().getID();

        // 3. 构造键值对：key = 路径, value = JSON 序列化的服务元信息
        String registerKey = ETCD_ROOT_PATH + serviceMetaInfo.getServiceNodeKey();
        ByteSequence key = ByteSequence.from(registerKey, StandardCharsets.UTF_8);
        ByteSequence value = ByteSequence.from(JSONUtil.toJsonStr(serviceMetaInfo), StandardCharsets.UTF_8);

        // 4. Put 时关联 Lease：这个 key 的生命周期和 Lease 绑定
        PutOption putOption = PutOption.builder().withLeaseId(leaseId).build();
        kvClient.put(key, value, putOption).get();

        // 5. 记录到本地缓存，用于续期和注销
        localRegisterNodeKeySet.add(registerKey);
        leaseIdCache.put(registerKey, leaseId);
    }

    @Override
    public void unRegister(ServiceMetaInfo serviceMetaInfo) throws ExecutionException, InterruptedException {
        String registerKey = ETCD_ROOT_PATH + serviceMetaInfo.getServiceNodeKey();
        // 从 Etcd 中删除 key
        kvClient.delete(ByteSequence.from(registerKey, StandardCharsets.UTF_8)).get();
        // 从本地缓存中移除
        localRegisterNodeKeySet.remove(registerKey);
        leaseIdCache.remove(registerKey);
    }

    @Override
    public List<ServiceMetaInfo> serviceDiscovery(String serviceKey) {
        // 优先从本地缓存获取（避免频繁访问 Etcd）
        List<ServiceMetaInfo> cachedServiceMetaInfoList = registryServiceMultiCache.readCache(serviceKey);
        if (CollUtil.isNotEmpty(cachedServiceMetaInfoList)) {
            return cachedServiceMetaInfoList;
        }

        // 缓存未命中，从 Etcd 前缀查询
        String searchPrefix = ETCD_ROOT_PATH + serviceKey;
        try {
            GetOption getOption = GetOption.builder().isPrefix(true).build();
            List<KeyValue> keyValues = kvClient.get(
                            ByteSequence.from(searchPrefix, StandardCharsets.UTF_8),
                            getOption)
                    .get()
                    .getKvs();

            // 解析每个 key-value 为 ServiceMetaInfo 对象
            return keyValues.stream()
                    .map(keyValue -> {
                        String serviceNodeKey = keyValue.getKey().toString(StandardCharsets.UTF_8);
                        // 对每个节点 key 注册 Watch 监听
                        watch(serviceKey, serviceNodeKey);
                        String value = keyValue.getValue().toString(StandardCharsets.UTF_8);
                        ServiceMetaInfo serviceMetaInfo = JSONUtil.toBean(value, ServiceMetaInfo.class);
                        // 写入本地缓存
                        registryServiceMultiCache.writeCache(serviceKey, serviceNodeKey, serviceMetaInfo);
                        return serviceMetaInfo;
                    })
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("获取服务列表失败", e);
        }
    }

    @Override
    public void heartBeat() {
        // 每 10 秒执行一次续期（Cron 表达式：从第 0 秒开始，每 10 秒一次）
        CronUtil.schedule("0/10 * * * * ?", (Task) () -> {
            // 遍历本节点所有已注册的 key
            for (String key : localRegisterNodeKeySet) {
                try {
                    Long leaseId = leaseIdCache.get(key);
                    if (leaseId == null) {
                        log.warn("key={} 无缓存的 leaseId，跳过续期", key);
                        continue;
                    }
                    // 用缓存的 leaseId 直接续期（比重新创建 Lease 高效得多）
                    Lease leaseClient = client.getLeaseClient();
                    leaseClient.keepAliveOnce(leaseId).get();
                    log.debug("key={} 续期成功，leaseId={}", key, leaseId);
                } catch (Exception e) {
                    log.error("{} 续期失败: {}", key, e.getMessage());
                }
            }
        });
        // 启用秒级 Cron 表达式支持
        CronUtil.setMatchSecond(true);
        CronUtil.start();
    }

    @Override
    public void watch(String serviceKey, String serviceNodeKey) {
        Watch watchClient = client.getWatchClient();
        // 只对未监听过的 key 注册 Watch（去重）
        boolean newWatch = watchingKeySet.add(serviceNodeKey);
        if (newWatch) {
            watchClient.watch(ByteSequence.from(serviceNodeKey, StandardCharsets.UTF_8), response -> {
                for (WatchEvent event : response.getEvents()) {
                    switch (event.getEventType()) {
                        // key 被删除时触发（节点下线或租约过期）
                        case DELETE:
                            // 清除本地缓存，下次 serviceDiscovery 会重新查询
                            registryServiceMultiCache.clearCache(serviceKey, serviceNodeKey);
                            break;
                        case PUT:
                        default:
                            break;
                    }
                }
            });
        }
    }

    @Override
    public void destroy() {
        log.info("当前节点下线");
        // 删除本节点注册的所有 key（主动下线，不要等租约过期）
        for (String key : localRegisterNodeKeySet) {
            try {
                kvClient.delete(ByteSequence.from(key, StandardCharsets.UTF_8)).get();
            } catch (Exception e) {
                throw new RuntimeException(key + "下线失败", e);
            }
        }

        // 清空本地缓存
        localRegisterNodeKeySet.clear();
        leaseIdCache.clear();

        // 释放 Etcd 客户端资源
        if (kvClient != null) {
            kvClient.close();
        }
        if (client != null) {
            client.close();
        }
    }
}
