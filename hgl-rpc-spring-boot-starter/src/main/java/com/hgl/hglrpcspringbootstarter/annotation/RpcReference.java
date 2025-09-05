package com.hgl.hglrpcspringbootstarter.annotation;

import com.hgl.hglrpc.constant.RpcConstant;
import com.hgl.hglrpc.fault.retry.RetryStrategyKeys;
import com.hgl.hglrpc.fault.tolerant.TolerantStrategyKeys;
import com.hgl.hglrpc.loadbalancer.LoadBalancerKeys;
import com.hgl.hglrpc.serializer.SerializerKeys;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @ClassName: RpcReference
 * @Package: com.hgl.hglrpcspringbootstarter.annotation
 * @Description: 服务消费者注解（用于注入服务）
 * @Author HGL
 * @Create: 2025/9/5 16:07
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RpcReference {
    /**
     * 服务接口类
     */
    Class<?> interfaceClass() default void.class;

    /**
     * 版本
     */
    String serviceVersion() default RpcConstant.DEFAULT_SERVICE_VERSION;

    /**
     * 序列化方式
     */
    String serializer() default SerializerKeys.JDK;

    /**
     * 角色
     */
    String role() default "provider";

    /**
     * 服务协议
     */
    String protocol() default "tcp";

    /**
     * 负载均衡器
     */
    String loadBalancer() default LoadBalancerKeys.ROUND_ROBIN;

    /**
     * 重试策略
     */
    String retryStrategy() default RetryStrategyKeys.NO;

    /**
     * 容错策略
     */
    String tolerantStrategy() default TolerantStrategyKeys.FAIL_FAST;

    /**
     * 模拟调用
     */
    boolean mock() default false;
}
