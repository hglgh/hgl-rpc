package com.hgl.hglrpc.registry;

import com.hgl.hglrpc.model.ServiceMetaInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @ClassName: RegistryServiceMultiCache
 * @Package: com.hgl.hglrpc.registry
 * @Description:
 * @Author HGL
 * @Create: 2025/9/2 16:58
 */
public class RegistryServiceMultiCache {

    /**
     * 服务缓存
     */
    Map<String, Map<String, ServiceMetaInfo>> serviceCache = new ConcurrentHashMap<>();

    /**
     * 写入缓存
     *
     * @param serviceKey      服务key
     * @param serviceNodeKey  服务节点key
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
     * 读取缓存
     *
     * @param serviceKey 服务key
     * @return 缓存
     */
    List<ServiceMetaInfo> readCache(String serviceKey) {
        Map<String, ServiceMetaInfo> metaInfoMap = this.serviceCache.get(serviceKey);
        return metaInfoMap == null ? null : new ArrayList<>(metaInfoMap.values());
    }

    /**
     * 清空缓存
     *
     * @param serviceKey 服务key
     */
    void clearCache(String serviceKey, String serviceNodeKey) {
        Map<String, ServiceMetaInfo> metaInfoMap = serviceCache.get(serviceKey);
        if (metaInfoMap != null) {
            metaInfoMap.remove(serviceNodeKey);
            if (metaInfoMap.isEmpty()) {
                serviceCache.remove(serviceKey);
            }
        }
    }
}
