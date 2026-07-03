package com.hgl.examplespringbootconsumer;

import com.hgl.hglrpcspringbootstarter.annotation.EnableRpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 版 RPC 消费者启动类 —— "只打电话，不接电话"
 *
 * <p>与提供者启动类的关键区别在于 {@code @EnableRpc(needServer = false)}：</p>
 * <ul>
 *   <li><b>提供者</b>：{@code @EnableRpc} —— 启动服务器，等待请求</li>
 *   <li><b>消费者</b>：{@code @EnableRpc(needServer = false)} —— 不启动服务器，只发起调用</li>
 * </ul>
 *
 * <h3>为什么消费者不需要服务器？</h3>
 * <pre>
 *   ┌──────────────┐                    ┌──────────────┐
 *   │    消费者      │   ── 发请求 ──▶   │    提供者      │
 *   │  (客户端)      │   ◀─ 收响应 ──   │  (服务端)      │
 *   │  不需要监听端口 │                    │  需要监听端口  │
 *   └──────────────┘                    └──────────────┘
 *
 *   消费者只是"打电话"的一方，不需要"装电话机"（开服务器）。
 *   如果不设置 needServer = false，框架会白白占用一个端口。
 * </pre>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 *   @SpringBootApplication
 *   @EnableRpc(needServer = false)  // 关键：消费者不需要启动 RPC 服务器
 *   public class MyConsumerApp {
 *       public static void main(String[] args) {
 *           SpringApplication.run(MyConsumerApp.class, args);
 *       }
 *   }
 *
 *   // 然后在任意 Bean 中使用 @RpcReference 注入远程服务
 *   @RpcReference
 *   private UserService userService;
 * }</pre>
 *
 * @author HGL
 * @see com.hgl.hglrpcspringbootstarter.annotation.EnableRpc —— RPC 启用注解
 * @see ExampleServiceImpl —— 使用 @RpcReference 注入远程服务的示例
 */
@SpringBootApplication
@EnableRpc(needServer = false)
public class ExampleSpringbootConsumerApplication {

    public static void main(String[] args) {
        // SpringApplication.run() 启动 Spring 容器
        // @EnableRpc(needServer = false) 确保：
        //   - 不启动 HTTP/TCP 服务器
        //   - 但仍会初始化 RPC 客户端（连接注册中心、配置序列化器等）
        //   - @RpcReference 注入的字段会被自动替换为远程代理
        SpringApplication.run(ExampleSpringbootConsumerApplication.class, args);
    }
}
