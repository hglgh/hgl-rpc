package com.hgl.hglrpc.registry;

import com.hgl.hglrpc.model.ServiceMetaInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 多服务注册缓存 —— "多本电话簿的副本"
 *
 * <p>与 {@link RegistryServiceCache} 不同，这个类支持同时缓存多个服务的节点信息。
 * 内部结构是两层 Map：
 * <pre>
 *   外层 Map：serviceKey → 内层 Map
 *   内层 Map：serviceNodeKey → ServiceMetaInfo
 *
 *   例如：
 *   "UserService:1.0" → {
 *       "UserService:1.0/192.168.1.1:8080" → ServiceMetaInfo{host=192.168.1.1, port=8080},
 *       "UserService:1.0/192.168.1.2:8080" → ServiceMetaInfo{host=192.168.1.2, port=8080}
 *   }
 *   "OrderService:1.0" → { ... }
 * </pre>
 *
 * <p>使用 ConcurrentHashMap 保证线程安全（多个请求线程并发读写）。
 *
 * @Author HGL
 * @Create: 2025/9/2 16:58
 */
public class RegistryServiceMultiCache {

    /**
     * 多服务缓存：serviceKey → (serviceNodeKey → ServiceMetaInfo)
     */
    Map<String, Map<String, ServiceMetaInfo>> serviceCache = new ConcurrentHashMap<>();

    /**
     * 写入缓存 —— "在电话簿副本中添加/更新一条记录"
     *
     * @param serviceKey      服务键名
     * @param serviceNodeKey  节点键名
     * @param serviceMetaInfo 服务元信息
     */
    void writeCache(String serviceKey, String serviceNodeKey, ServiceMetaInfo serviceMetaInfo) {
        if (!serviceCache.containsKey(serviceKey)) {
            Map<String, ServiceMetaInfo> metaInfoMap = new HashMap<>();
            metaInfoMap.put(serviceNodeKey, serviceMetaInfo);
            serviceCache.put(serviceKey, metaInfoMap);
        } else {
            serviceCache.get(serviceKey).put(serviceNodeKey, serviceMetaInfo);
        }
    }

    /**
     * 读取缓存 —— "查找某个服务的所有节点"
     *
     * @param serviceKey 服务键名
     * @return 节点列表（从 Map 的 values 转换而来），无缓存返回 null
     */
    List<ServiceMetaInfo> readCache(String serviceKey) {
        Map<String, ServiceMetaInfo> metaInfoMap = this.serviceCache.get(serviceKey);
        return metaInfoMap == null ? null : new ArrayList<>(metaInfoMap.values());
    }

    /**
     * 清除某个节点的缓存 —— "某个节点下线了，从副本中删除"
     *
     * <p>当 Watch 监听到节点删除事件时调用。
     * 如果某个服务的所有节点都被清除了，连外层的 key 也一并删除。
     *
     * @param serviceKey     服务键名
     * @param serviceNodeKey 节点键名
     */
    void clearCache(String serviceKey, String serviceNodeKey) {
        Map<String, ServiceMetaInfo> metaInfoMap = serviceCache.get(serviceKey);
        if (metaInfoMap != null) {
            metaInfoMap.remove(serviceNodeKey);
            // 如果该服务的所有节点都没了，清理外层 key
            if (metaInfoMap.isEmpty()) {
                serviceCache.remove(serviceKey);
            }
        }
    }
}
