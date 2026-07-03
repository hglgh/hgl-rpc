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
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;

import java.util.Map;

/**
 * RPC 提供者引导类（组件扫描方式）—— "批量登记所有商家"
 *
 * <p>实现 {@link SmartLifecycle} 接口，在 Spring 容器完全就绪后（所有 Bean 初始化完成），
 * 一次性扫描所有带 {@code @RpcService} 注解的 Bean，批量注册到注册中心。
 *
 * <p>与 {@link RpcProviderBootstrap}（BeanPostProcessor 方式）的区别：
 * <pre>
 *   ┌───────────────────────────────────────────────────────────────────┐
 *   │ 方式                   | 时机              | 优势                │
 *   ├───────────────────────────────────────────────────────────────────┤
 *   │ RpcProviderBootstrap   | Bean 初始化后逐个  | 早注册、早发现       │
 *   │ RpcProviderWithScan    | 容器就绪后批量     | 依赖注入已完成       │
 *   └───────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/9/8 10:20
 */
@Slf4j
public class RpcProviderWithScanBootstrap implements SmartLifecycle, ApplicationContextAware {

    private ApplicationContext applicationContext;

    private boolean isRunning = false;

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void start() {
        // 扫描所有带 @RpcService 注解的 Bean
        Map<String, Object> rpcServiceBeans = applicationContext.getBeansWithAnnotation(RpcService.class);

        for (Object serviceBean : rpcServiceBeans.values()) {
            Class<?> beanClass = serviceBean.getClass();
            RpcService rpcService = beanClass.getAnnotation(RpcService.class);
            if (rpcService != null) {
                // 1. 获取服务基本信息
                Class<?> interfaceClass = rpcService.interfaceClass();
                if (interfaceClass == void.class) {
                    interfaceClass = beanClass.getInterfaces()[0];
                }
                String serviceName = interfaceClass.getName();
                String serviceVersion = rpcService.serviceVersion();

                // 2. 本地注册（注册实例，不是 Class）
                LocalRegistry.register(serviceName, serviceBean);

                // 3. 注册到远程注册中心
                final RpcConfig rpcConfig = RpcApplication.getRpcConfig();
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
     * 注册服务到远程注册中心
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
