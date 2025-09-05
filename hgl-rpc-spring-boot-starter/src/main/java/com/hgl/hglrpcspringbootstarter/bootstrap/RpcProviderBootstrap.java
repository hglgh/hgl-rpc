package com.hgl.hglrpcspringbootstarter.bootstrap;

import com.hgl.hglrpc.RpcApplication;
import com.hgl.hglrpc.config.RegistryConfig;
import com.hgl.hglrpc.config.RpcConfig;
import com.hgl.hglrpc.model.ServiceMetaInfo;
import com.hgl.hglrpc.registry.LocalRegistry;
import com.hgl.hglrpc.registry.Registry;
import com.hgl.hglrpc.registry.RegistryFactory;
import com.hgl.hglrpcspringbootstarter.annotation.RpcService;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * @ClassName: RpcProviderBootstrap
 * @Package: com.hgl.hglrpcspringbootstarter.bootstrap
 * @Description: Rpc 服务提供者启动
 * @Author HGL
 * @Create: 2025/9/5 16:21
 */
public class RpcProviderBootstrap implements BeanPostProcessor {
    /**
     * Bean 初始化后执行，注册服务
     *
     * @param bean     对象
     * @param beanName 对象名
     * @return 对象
     * @throws BeansException 异常
     */
    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        RpcService rpcService = beanClass.getAnnotation(RpcService.class);
        if (rpcService != null) {
            // 需要注册服务
            // 1. 获取服务基本信息
            Class<?> interfaceClass = rpcService.interfaceClass();
            // 默认值处理
            if (interfaceClass == void.class) {
                interfaceClass = beanClass.getInterfaces()[0];
            }
            String serviceName = interfaceClass.getName();
            String serviceVersion = rpcService.serviceVersion();
            // 2. 注册服务
            // 本地注册
            LocalRegistry.register(serviceName, beanClass);
            // 全局配置
            final RpcConfig rpcConfig = RpcApplication.getRpcConfig();
            // 注册服务到注册中心
            registerService(rpcConfig, serviceName, serviceVersion);
        }
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }

    /**
     * 注册服务到注册中心
     *
     * @param rpcConfig RPC配置
     * @param serviceName 服务名称
     * @param serviceVersion 服务版本
     */
    private void registerService(RpcConfig rpcConfig, String serviceName, String serviceVersion) {
        RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
        Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName(serviceName);
        serviceMetaInfo.setServiceVersion(serviceVersion);
        serviceMetaInfo.setServiceHost(rpcConfig.getServerHost());
        serviceMetaInfo.setServicePort(rpcConfig.getServerPort());
        try {
            registry.register(serviceMetaInfo);
        } catch (Exception e) {
            throw new RuntimeException(serviceName + " 服务注册失败", e);
        }
    }

}
