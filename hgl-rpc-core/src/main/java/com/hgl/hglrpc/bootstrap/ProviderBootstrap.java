package com.hgl.hglrpc.bootstrap;

import com.hgl.hglrpc.RpcApplication;
import com.hgl.hglrpc.config.RegistryConfig;
import com.hgl.hglrpc.config.RpcConfig;
import com.hgl.hglrpc.model.ServiceMetaInfo;
import com.hgl.hglrpc.model.ServiceRegisterInfo;
import com.hgl.hglrpc.registry.LocalRegistry;
import com.hgl.hglrpc.registry.Registry;
import com.hgl.hglrpc.registry.RegistryFactory;
import com.hgl.hglrpc.server.VertxServerFactory;

import java.util.List;

/**
 * @ClassName: ProviderBootstrap
 * @Package: com.hgl.hglrpc.bootstrap
 * @Description: 服务提供者初始化
 * @Author HGL
 * @Create: 2025/9/5 15:41
 */
public class ProviderBootstrap {
    public static void init(List<ServiceRegisterInfo<?>> serviceRegisterInfoList) {
        // 全局配置
        final RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        for (ServiceRegisterInfo<?> serviceRegisterInfo : serviceRegisterInfoList) {
            String serviceName = serviceRegisterInfo.getServiceName();
            // 注册服务到本地缓存
            LocalRegistry.register(serviceName, serviceRegisterInfo.getImplClass());
            // 注册服务到注册中心
            registryToCenter(rpcConfig, serviceName);
        }


        // 启动 web 服务
        VertxServerFactory.getInstance(rpcConfig.getProtocol()).doStart(rpcConfig.getServerPort());
    }

    /**
     * 注册服务到注册中心
     *
     * @param serviceName 服务名
     */
    private static void registryToCenter(RpcConfig rpcConfig, String serviceName) {
        RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
        Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName(serviceName);
        serviceMetaInfo.setServiceHost(rpcConfig.getServerHost());
        serviceMetaInfo.setServicePort(rpcConfig.getServerPort());
        try {
            registry.register(serviceMetaInfo);
        } catch (Exception e) {
            throw new RuntimeException(serviceName + " 服务注册失败", e);
        }
    }
}
