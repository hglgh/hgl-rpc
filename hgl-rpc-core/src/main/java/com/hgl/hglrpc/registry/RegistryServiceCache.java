package com.hgl.hglrpc.registry;

import com.hgl.hglrpc.model.ServiceMetaInfo;

import java.util.List;

/**
 * @ClassName: RegistryServiceCache
 * @Package: com.hgl.hglrpc.registry
 * @Description: 注册中心服务本地缓存
 * @Author HGL
 * @Create: 2025/9/2 16:53
 */
public class RegistryServiceCache {

    /**
     * 服务缓存
     */
    List<ServiceMetaInfo> serviceCache;

    /**
     * 写缓存
     *
     * @param newServiceCache 新服务缓存
     */
    void writeCache(List<ServiceMetaInfo> newServiceCache) {
        this.serviceCache = newServiceCache;
    }

    /**
     * 读缓存
     *
     * @return 服务缓存
     */
    List<ServiceMetaInfo> readCache() {
        return this.serviceCache;
    }

    /**
     * 清空缓存
     */
    void clearCache() {
        this.serviceCache = null;
    }
}
