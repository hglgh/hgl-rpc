package com.hgl.hglrpcspringbootstarter.bootstrap;

import com.hgl.hglrpc.RpcApplication;
import com.hgl.hglrpc.config.RegistryConfig;
import com.hgl.hglrpc.config.RpcConfig;
import com.hgl.hglrpc.model.ServiceMetaInfo;
import com.hgl.hglrpc.registry.LocalRegistry;
import com.hgl.hglrpc.registry.Registry;
import com.hgl.hglrpc.registry.RegistryFactory;
import com.hgl.hglrpcspringbootstarter.annotation.RpcService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;

import java.util.Map;

/**
 * @ClassName: RpcProviderWithScanBootstrap
 * @Package: com.hgl.hglrpcspringbootstarter.bootstrap
 * @Description:  RPC服务启动引导类使用了组件扫描
 * @Author HGL
 * @Create: 2025/9/8 10:20
 */
@Slf4j
public class RpcProviderWithScanBootstrap implements SmartLifecycle, ApplicationContextAware {
    private ApplicationContext applicationContext;

    private boolean isRunning = false;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void start() {
        // 扫描所有带 RpcService 注解的 Bean
        Map<String, Object> rpcServiceBeans = applicationContext.getBeansWithAnnotation(RpcService.class);

        for (Object serviceBean : rpcServiceBeans.values()) {
            Class<?> beanClass = serviceBean.getClass();
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
                log.info("Registered RPC service: {} with version: {}", serviceName, serviceVersion);
            }
        }
        isRunning = true;
    }

    @Override
    public void stop() {
        isRunning = false;
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }

    /**
     * 注册服务到注册中心
     *
     * @param rpcConfig      RPC配置
     * @param serviceName    服务名称
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