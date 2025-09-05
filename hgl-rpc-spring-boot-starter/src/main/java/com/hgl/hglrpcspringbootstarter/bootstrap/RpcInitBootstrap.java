package com.hgl.hglrpcspringbootstarter.bootstrap;

import com.hgl.hglrpc.RpcApplication;
import com.hgl.hglrpc.config.RpcConfig;
import com.hgl.hglrpc.server.VertxServer;
import com.hgl.hglrpc.server.VertxServerFactory;
import com.hgl.hglrpc.server.tcp.VertxTcpServer;
import com.hgl.hglrpcspringbootstarter.annotation.EnableRpc;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import java.util.Map;
import java.util.Objects;

/**
 * @ClassName: RpcInitBootstrap
 * @Package: com.hgl.hglrpcspringbootstarter.bootstrap
 * @Description: Rpc 框架启动
 * @Author HGL
 * @Create: 2025/9/5 16:10
 */
@Slf4j
public class RpcInitBootstrap implements ImportBeanDefinitionRegistrar {
    @Override
    public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
        // 获取 EnableRpc 注解的属性值
        Map<String, Object> annotationAttributes = Objects.requireNonNull(importingClassMetadata.getAnnotationAttributes(EnableRpc.class.getName()));
        boolean needServer = (boolean) annotationAttributes.get("needServer");

        // 全局配置
        final RpcConfig rpcConfig = RpcApplication.getRpcConfig();

        // 启动服务器
        if (needServer) {
            String protocol = (String) annotationAttributes.get("protocol");
            rpcConfig.setProtocol(protocol);
            log.info("启动协议：{}", protocol);
            VertxServer vertxServer = VertxServerFactory.getInstance(protocol);
            vertxServer.doStart(rpcConfig.getServerPort());
        } else {
            log.info("不启动 server");
        }
    }
}
