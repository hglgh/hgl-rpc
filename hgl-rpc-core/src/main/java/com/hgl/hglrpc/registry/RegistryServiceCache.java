package com.hgl.hglrpc.registry;

import com.hgl.hglrpc.model.ServiceMetaInfo;

import java.util.List;

/**
 * 注册中心服务缓存（单服务） —— "单个服务的电话簿副本"
 *
 * <p>这是一个简单的缓存类，存储单个服务的所有节点信息。
 * 每次服务发现时，优先从缓存读取，避免频繁访问注册中心。
 *
 * <p>注意：这个类已被 {@link RegistryServiceMultiCache} 取代。
 * RegistryServiceMultiCache 支持缓存多个服务（按 serviceKey 区分），
 * 而这个类只能缓存一个服务。保留是为了向后兼容。
 *
 * @Author HGL
 * @Create: 2025/9/2 16:53
 */
public class RegistryServiceCache {

    /** 服务节点列表缓存 */
    List<ServiceMetaInfo> serviceCache;

    /**
     * 写入缓存
     *
     * @param newServiceCache 新的服务节点列表
     */
    void writeCache(List<ServiceMetaInfo> newServiceCache) {
        this.serviceCache = newServiceCache;
    }

    /**
     * 读取缓存
     *
     * @return 缓存的服务节点列表
     */
    List<ServiceMetaInfo> readCache() {
        return this.serviceCache;
    }

    /**
     * 清空缓存（节点变更时调用）
     */
    void clearCache() {
        this.serviceCache = null;
    }
}
