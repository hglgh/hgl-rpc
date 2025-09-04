package com.hgl.example.provider;

import com.hgl.example.common.service.UserService;
import com.hgl.hglrpc.RpcApplication;
import com.hgl.hglrpc.config.RegistryConfig;
import com.hgl.hglrpc.config.RpcConfig;
import com.hgl.hglrpc.model.ServiceMetaInfo;
import com.hgl.hglrpc.registry.LocalRegistry;
import com.hgl.hglrpc.registry.Registry;
import com.hgl.hglrpc.registry.RegistryFactory;
import com.hgl.hglrpc.server.VertxServerFactory;

/**
 * @ClassName: ProviderExample
 * @Package: com.hgl.example.provider
 * @Description:
 * @Author HGL
 * @Create: 2025/9/1 11:07
 */
public class ProviderExample {
    public static void main(String[] args) {

        String serviceName = UserService.class.getName();
        // 注册服务到本地缓存
        LocalRegistry.register(serviceName, UserServiceImpl.class);

        // 注册服务到注册中心
        RpcConfig rpcConfig = registryToCenter(serviceName);

        // 启动 web 服务
        VertxServerFactory.getInstance(rpcConfig.getProtocol()).doStart(rpcConfig.getServerPort());
    }

    /**
     * 注册服务到注册中心
     *
     * @param serviceName 服务名
     * @return RpcConfig
     */
    private static RpcConfig registryToCenter(String serviceName) {
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
        Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName(serviceName);
        serviceMetaInfo.setServiceHost(rpcConfig.getServerHost());
        serviceMetaInfo.setServicePort(rpcConfig.getServerPort());
        try {
            registry.register(serviceMetaInfo);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return rpcConfig;
    }
}
