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
 * 启用 RPC 功能的注解 —— "开门营业的一键开关"
 *
 * <p>在 Spring Boot 启动类上添加此注解，即可启用 RPC 框架。
 * 它通过 {@code @Import} 导入了三个核心启动引导类：
 *
 * <pre>
 *   ┌─────────────────────────────────────────────────────────────────┐
 *   │                    @EnableRpc                                   │
 *   │                     │                                           │
 *   │    ┌────────────────┼────────────────┐                          │
 *   │    ▼                ▼                ▼                          │
 *   │ RpcInitBootstrap  RpcProviderBootstrap  RpcConsumerBootstrap    │
 *   │ 初始化框架+启动服务器  注册服务到注册中心    注入服务代理           │
 *   └─────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>使用示例：
 * <pre>
 *   {@literal @}SpringBootApplication
 *   {@literal @}EnableRpc(needServer = true, protocol = "tcp")
 *   public class ProviderApplication {
 *       public static void main(String[] args) {
 *           SpringApplication.run(ProviderApplication.class, args);
 *       }
 *   }
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/9/5 16:05
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Import({RpcInitBootstrap.class, RpcProviderBootstrap.class, RpcConsumerBootstrap.class})
public @interface EnableRpc {

    /**
     * 是否需要启动服务器 —— "你是商家还是顾客？"
     *
     * <p>true = 提供者（需要启动服务器接收请求）
     * <p>false = 消费者（只发请求，不需要服务器）
     */
    boolean needServer() default true;

    /**
     * 服务协议 —— "用什么方式通信？"
     *
     * <p>可选值："tcp" 或 "http"，仅在 needServer=true 时有效。
     */
    String protocol() default "tcp";
}
