package com.hgl.hglrpcspringbootstarter.bootstrap;

import com.hgl.hglrpc.RpcApplication;
import com.hgl.hglrpc.config.RpcConfig;
import com.hgl.hglrpc.server.VertxServer;
import com.hgl.hglrpc.server.VertxServerFactory;
import com.hgl.hglrpcspringbootstarter.annotation.EnableRpc;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;
import java.util.Objects;

/**
 * RPC 初始化引导类 —— "框架的总开关"
 *
 * <p>当 Spring 容器启动时，{@code @EnableRpc} 通过 {@code @Import} 触发本类。
 * 它负责：
 * <pre>
 *   1. 读取 @EnableRpc 注解的属性（needServer、protocol）
 *   2. 初始化 RPC 全局配置
 *   3. 如果是提供者（needServer=true），启动网络服务器
 * </pre>
 *
 * <p>实现 {@link ImportBeanDefinitionRegistrar} 接口，
 * 可以在 Spring 注册 Bean 定义阶段执行自定义逻辑（比 {@code @PostConstruct} 更早）。
 *
 * @Author HGL
 * @Create: 2025/9/5 16:10
 */
@Slf4j
public class RpcInitBootstrap implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, @NonNull BeanDefinitionRegistry registry) {
        // 1. 获取 @EnableRpc 注解的属性值
        Map<String, Object> annotationAttributes = Objects.requireNonNull(
                importingClassMetadata.getAnnotationAttributes(EnableRpc.class.getName()));
        boolean needServer = (boolean) annotationAttributes.get("needServer");

        // 2. 初始化 RPC 全局配置（读取 application.yml / application.properties）
        final RpcConfig rpcConfig = RpcApplication.getRpcConfig();

        // 3. 如果需要启动服务器
        if (needServer) {
            String protocol = (String) annotationAttributes.get("protocol");
            rpcConfig.setProtocol(protocol);
            log.info("启动协议：{}", protocol);
            // 通过 SPI 获取对应协议的服务器实例并启动
            VertxServer vertxServer = VertxServerFactory.getInstance(protocol);
            vertxServer.doStart(rpcConfig.getServerPort());
        } else {
            log.info("不启动 server");
        }
    }
}
