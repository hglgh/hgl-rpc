package com.hgl.hglrpcspringbootstarter.annotation;

import com.hgl.hglrpc.constant.RpcConstant;
import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * RPC 服务提供者注解 —— "我是做 RPC 生意的商家"
 *
 * <p>标注在服务实现类上，表示这个类对外提供 RPC 服务。
 * 框架在启动时会自动扫描带此注解的 Bean，将其注册到注册中心。
 *
 * <p>使用示例：
 * <pre>
 *   {@literal @}RpcService(interfaceClass = UserService.class, serviceVersion = "1.0")
 *   public class UserServiceImpl implements UserService {
 *       // 业务实现...
 *   }
 * </pre>
 *
 * <p>注意：{@code @Component} 注解使得 Spring 会自动创建该类的 Bean。
 * {@code @RpcService} 还能作为 {@code @Component} 的"标记"，
 * 让 {@link com.hgl.hglrpcspringbootstarter.bootstrap.RpcProviderWithScanBootstrap}
 * 通过 {@code getBeansWithAnnotation(RpcService.class)} 找到它。
 *
 * @Author HGL
 * @Create: 2025/9/5 16:06
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Component  // 同时让 Spring 管理该 Bean
public @interface RpcService {

    /**
     * 服务接口类 —— "你对外宣称做什么生意"
     *
     * <p>默认为 void.class，表示自动取该类实现的第一个接口。
     */
    Class<?> interfaceClass() default void.class;

    /**
     * 服务版本 —— "你的店是第几代"
     *
     * <p>不同版本可以同时存在，消费者通过版本号路由到正确的服务。
     */
    String serviceVersion() default RpcConstant.DEFAULT_SERVICE_VERSION;
}
