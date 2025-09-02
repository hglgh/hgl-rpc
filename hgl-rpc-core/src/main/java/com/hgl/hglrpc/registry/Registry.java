package com.hgl.hglrpc.registry;

import com.hgl.hglrpc.config.RegistryConfig;
import com.hgl.hglrpc.model.ServiceMetaInfo;

import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * @ClassName: Registry
 * @Package: com.hgl.hglrpc.registry
 * @Description: 注册中心
 * @Author HGL
 * @Create: 2025/9/2 13:50
 */
public interface Registry {

    /**
     * 初始化
     *
     * @param registryConfig 注册中心配置
     */
    void init(RegistryConfig registryConfig);

    /**
     * 注册服务（服务端）
     *
     * @param serviceMetaInfo 服务元信息
     */
    void register(ServiceMetaInfo serviceMetaInfo) throws Exception;

    /**
     * 注销服务（服务端）
     *
     * @param serviceMetaInfo 服务元信息
     */
    void unRegister(ServiceMetaInfo serviceMetaInfo) throws ExecutionException, InterruptedException;

    /**
     * 服务发现（获取某服务的所有节点，消费端）
     *
     * @param serviceKey 服务键名
     * @return 服务元信息列表
     */
    List<ServiceMetaInfo> serviceDiscovery(String serviceKey);

    /**
     * 服务销毁
     */
    void destroy();
}
