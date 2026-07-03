package com.hgl.hglrpcspringbootstarter.bootstrap;

import com.hgl.hglrpc.RpcApplication;
import com.hgl.hglrpc.config.RegistryConfig;
import com.hgl.hglrpc.config.RpcConfig;
import com.hgl.hglrpc.model.ServiceMetaInfo;
import com.hgl.hglrpc.registry.LocalRegistry;
import com.hgl.hglrpc.registry.Registry;
import com.hgl.hglrpc.registry.RegistryFactory;
import com.hgl.hglrpcspringbootstarter.annotation.RpcService;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * RPC 提供者引导类（BeanPostProcessor 方式）—— "逐个登记商家"
 *
 * <p>实现 {@link BeanPostProcessor} 接口，在每个 Bean 初始化完成后检查是否带有
 * {@code @RpcService} 注解。如果有，就将该 Bean 的服务信息注册到注册中心。
 *
 * <p>工作流程：
 * <pre>
 *   Spring 创建 Bean → BeanPostProcessor.postProcessAfterInitialization()
 *       → 检查是否有 @RpcService
 *       → 提取服务名、版本号
 *       → LocalRegistry.register()（本地注册，存实例）
 *       → Registry.register()（注册中心注册，如 Etcd）
 * </pre>
 *
 * <p>注意：这种方式在 Bean 逐个初始化时就完成注册，
 * 另一种方式 {@link RpcProviderWithScanBootstrap} 是在所有 Bean 就绪后批量扫描注册。
 *
 * @Author HGL
 * @Create: 2025/9/5 16:21
 */
public class RpcProviderBootstrap implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, @NonNull String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();
        RpcService rpcService = beanClass.getAnnotation(RpcService.class);

        if (rpcService != null) {
            // 1. 获取服务基本信息
            Class<?> interfaceClass = rpcService.interfaceClass();
            if (interfaceClass == void.class) {
                // 默认取第一个实现接口
                interfaceClass = beanClass.getInterfaces()[0];
            }
            String serviceName = interfaceClass.getName();
            String serviceVersion = rpcService.serviceVersion();

            // 2. 本地注册（存实例，不存 Class，避免每次请求反射创建新对象）
            LocalRegistry.register(serviceName, bean);

            // 3. 注册到远程注册中心（如 Etcd）
            final RpcConfig rpcConfig = RpcApplication.getRpcConfig();
            registerService(rpcConfig, serviceName, serviceVersion);
        }
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
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
