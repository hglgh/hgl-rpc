package com.hgl.hglrpcspringbootstarter.annotation;

import com.hgl.hglrpcspringbootstarter.bootstrap.RpcConsumerBootstrap;
import com.hgl.hglrpcspringbootstarter.bootstrap.RpcInitBootstrap;
import com.hgl.hglrpcspringbootstarter.bootstrap.RpcProviderBootstrap;
import org.springframework.context.annotation.Import;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @ClassName: EnableRpc
 * @Package: com.hgl.hglrpcspringbootstarter.annotation
 * @Description: 启用 Rpc 注解
 * @Author HGL
 * @Create: 2025/9/5 16:05
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({RpcInitBootstrap.class, RpcProviderBootstrap.class, RpcConsumerBootstrap.class})
public @interface EnableRpc {
    /**
     * 需要启动 server
     *
     * @return boolean
     */
    boolean needServer() default true;

    /**
     * 服务协议 (仅在 needServer 为 true 时有效)
     *
     * @return String
     */
    String protocol() default "tcp";
}
