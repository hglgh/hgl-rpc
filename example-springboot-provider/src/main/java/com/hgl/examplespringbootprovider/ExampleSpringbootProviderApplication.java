package com.hgl.examplespringbootprovider;

import com.hgl.hglrpcspringbootstarter.annotation.EnableRpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 版 RPC 提供者启动类 —— "自动挡中的自动挡"
 *
 * <p>对比原生 API 版本（{@link com.hgl.example.provider.ProviderExample}），
 * Spring Boot 版本的启动流程被极大简化了：</p>
 *
 * <h3>原生 API vs Spring Boot</h3>
 * <pre>
 *   ┌──────────────────────────────────────────────────────────────┐
 *   │  原生 API 版本                    │  Spring Boot 版本          │
 *   ├──────────────────────────────────┼───────────────────────────┤
 *   │ 1. 手动准备 ServiceRegisterInfo   │ 1. 在实现类上加 @RpcService │
 *   │ 2. 手动调用 ProviderBootstrap     │ 2. 在启动类加 @EnableRpc   │
 *   │ 3. 手动配置各种参数               │ 3. application.yml 配置    │
 *   ├──────────────────────────────────┼───────────────────────────┤
 *   │ 更灵活，适合学习原理              │ 更简洁，适合生产开发        │
 *   └──────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>{@code @EnableRpc} 注解的作用</h3>
 * <p>这个注解是"开关"，按下后框架自动完成：</p>
 * <ul>
 *   <li>扫描所有带 {@code @RpcService} 注解的类并注册</li>
 *   <li>启动 HTTP/TCP 服务器监听请求</li>
 *   <li>将服务注册到注册中心</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 *   // 1. 启动类加 @EnableRpc
 *   @SpringBootApplication
 *   @EnableRpc
 *   public class MyProviderApp {
 *       public static void main(String[] args) {
 *           SpringApplication.run(MyProviderApp.class, args);
 *       }
 *   }
 *
 *   // 2. 服务实现类加 @RpcService（见 UserServiceImpl）
 *   @RpcService
 *   public class UserServiceImpl implements UserService { ... }
 *
 *   // 3. application.yml 配置 RPC 参数
 *   // hglrpc:
 *   //   serverPort: 8080
 *   //   registryAddress: http://localhost:2379
 * }</pre>
 *
 * @author HGL
 * @see com.hgl.hglrpcspringbootstarter.annotation.EnableRpc —— RPC 启用注解
 * @see com.hgl.examplespringbootprovider.UserServiceImpl —— 带 @RpcService 的服务实现
 */
@SpringBootApplication
@EnableRpc
public class ExampleSpringbootProviderApplication {

    public static void main(String[] args) {
        // SpringApplication.run() 会：
        //   1) 创建 Spring 容器、扫描注解
        //   2) 触发 @EnableRpc 注入的 BeanPostProcessor
        //   3) BeanPostProcessor 发现 @RpcService 标注的 Bean，自动注册为 RPC 服务
        //   4) 启动内嵌服务器
        SpringApplication.run(ExampleSpringbootProviderApplication.class, args);
    }
}
