package com.hgl.examplespringbootconsumer;

import com.hgl.example.common.model.User;
import com.hgl.example.common.service.UserService;
import com.hgl.hglrpcspringbootstarter.annotation.RpcReference;
import org.springframework.stereotype.Service;

/**
 * 消费者业务服务 —— "用注解注入远程服务"
 *
 * <p>这个类展示了 Spring Boot 消费者最优雅的用法：
 * 用 {@code @RpcReference} 注解注入远程服务代理，就像注入本地 Bean 一样简单。</p>
 *
 * <h3>{@code @RpcReference} 注解的魔法</h3>
 * <pre>
 *   ┌───────────────────────────────────────────────────────────────┐
 *   │  Spring 容器初始化时：                                         │
 *   │                                                               │
 *   │  1. 发现 ExampleServiceImpl 中的 userService 字段              │
 *   │     上有 @RpcReference 注解                                    │
 *   │          ↓                                                    │
 *   │  2. 框架自动创建 UserService 的远程代理对象                      │
 *   │     (类似 ServiceProxyFactory.getProxy(UserService.class))     │
 *   │          ↓                                                    │
 *   │  3. 通过反射注入到 userService 字段                             │
 *   │          ↓                                                    │
 *   │  4. 调用 userService.getUser() 时，                            │
 *   │     实际走的是远程 RPC 调用！                                   │
 *   └───────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>与原生版本对比</h3>
 * <pre>{@code
 *   // ======= 原生 API 版（需要手动获取代理）=======
 *   ConsumerBootstrap.init();
 *   UserService userService = ServiceProxyFactory.getProxy(UserService.class);
 *
 *   // ======= Spring Boot 版（注解自动注入）========
 *   @RpcReference          // ← 就这一行！
 *   private UserService userService;
 * }</pre>
 *
 * <h3>原理对比</h3>
 * <table>
 *   <tr><th>对比项</th><th>原生版</th><th>Spring Boot 版</th></tr>
 *   <tr><td>获取代理</td><td>手动调用 ServiceProxyFactory</td><td>@RpcReference 自动注入</td></tr>
 *   <tr><td>初始化</td><td>手动调用 ConsumerBootstrap.init()</td><td>@EnableRpc 自动初始化</td></tr>
 *   <tr><td>服务发现</td><td>框架内置逻辑</td><td>框架内置 + Spring 生命周期</td></tr>
 * </table>
 *
 * @author HGL
 * @see com.hgl.hglrpcspringbootstarter.annotation.RpcReference —— 远程服务引用注解
 * @see ExampleSpringbootConsumerApplication —— 消费者启动类
 */
@Service
public class ExampleServiceImpl {

    /**
     * 远程服务引用 —— 看起来是本地字段，实际是远程代理
     *
     * <p>这个字段在运行时会被替换为一个代理对象。
     * 调用 {@code userService.getUser(user)} 时：
     * <ol>
     *   <li>代理拦截方法调用</li>
     *   <li>构造 RpcRequest 发送到远程提供者</li>
     *   <li>等待并接收 RpcResponse</li>
     *   <li>返回结果给调用者</li>
     * </ol>
     * 整个过程对业务代码完全透明！</p>
     */
    @RpcReference
    private UserService userService;

    /**
     * 业务测试方法 —— 演示如何调用远程 RPC 服务
     *
     * <p>从代码上看，这和调用本地 Service 没有任何区别。
     * 但底层发生了完整的 RPC 调用链路。
     * 这就是 RPC 框架的核心价值：<b>让分布式调用像本地调用一样简单</b>。</p>
     */
    public void test() {
        // 构造请求参数
        User user = new User();
        user.setName("hgl");

        // 调用远程服务 —— 看起来是本地方法调用
        // 实际上经过了：代理 → 序列化 → 网络传输 → 反序列化 → 远程执行 → 响应返回
        User resultUser = userService.getUser(user);
        System.out.println(resultUser.getName());
    }
}
