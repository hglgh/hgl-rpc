package com.hgl.hglrpcspringbootstarter.annotation;

import com.hgl.hglrpc.constant.RpcConstant;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @ClassName: RpcService
 * @Package: com.hgl.hglrpcspringbootstarter.annotation
 * @Description: 服务提供者注解（用于注册服务），在‌需要注册和提供的服务类上使用。
 * @Author HGL
 * @Create: 2025/9/5 16:06
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component
public @interface RpcService {
    /**
     * 服务接口类
     */
    Class<?> interfaceClass() default void.class;

    /**
     * 版本
     */
    String serviceVersion() default RpcConstant.DEFAULT_SERVICE_VERSION;
}
