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
 * RPC 服务消费者注解 —— "我要调用远程服务"
 *
 * <p>标注在字段上，框架会自动为该字段注入一个代理对象。
 * 调用代理对象的方法时，实际会发起 RPC 远程调用。
 *
 * <p>使用示例：
 * <pre>
 *   {@literal @}RpcReference(loadBalancer = "roundRobin", retryStrategy = "fixedInterval")
 *   private UserService userService;
 *
 *   // 之后直接调用，透明远程调用
 *   User user = userService.getUserById(1L);
 * </pre>
 *
 * <p>支持的配置项：
 * <pre>
 *   ┌──────────────────┬────────────────────┬─────────────────────────┐
 *   │ 属性              │ 默认值              │ 说明                     │
 *   ├──────────────────┼────────────────────┼─────────────────────────┤
 *   │ interfaceClass   │ void.class（自动）   │ 服务接口类               │
 *   │ serviceVersion   │ "1.0"              │ 服务版本                 │
 *   │ serializer       │ "jdk"              │ 序列化方式               │
 *   │ protocol         │ "tcp"              │ 通信协议                 │
 *   │ loadBalancer     │ "roundRobin"       │ 负载均衡策略             │
 *   │ retryStrategy    │ "no"               │ 重试策略                 │
 *   │ tolerantStrategy │ "failFast"         │ 容错策略                 │
 *   │ mock             │ false              │ 是否模拟调用             │
 *   └──────────────────┴────────────────────┴─────────────────────────┘
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/9/5 16:07
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface RpcReference {

    /** 服务接口类（默认自动取字段类型） */
    Class<?> interfaceClass() default void.class;

    /** 服务版本 */
    String serviceVersion() default RpcConstant.DEFAULT_SERVICE_VERSION;

    /** 序列化方式 */
    String serializer() default SerializerKeys.JDK;

    /** 角色 */
    String role() default "provider";

    /** 服务协议（tcp / http） */
    String protocol() default "tcp";

    /** 负载均衡器 */
    String loadBalancer() default LoadBalancerKeys.ROUND_ROBIN;

    /** 重试策略 */
    String retryStrategy() default RetryStrategyKeys.NO;

    /** 容错策略 */
    String tolerantStrategy() default TolerantStrategyKeys.FAIL_FAST;

    /** 模拟调用（true 时返回 null，用于测试） */
    boolean mock() default false;
}
