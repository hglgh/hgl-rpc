package com.hgl.example.consumer;

import com.hgl.example.common.model.User;
import com.hgl.example.common.service.UserService;
import com.hgl.example.consumer.proxy.UserServiceProxy;

/**
 * 简易版消费者示例 —— "手写代理"的入门教程
 *
 * <p>这个示例展示了 RPC 调用的底层原理：手动创建一个代理类来模拟框架的代理行为。
 * 理解了这个，你就理解了 RPC 框架的核心秘密。</p>
 *
 * <h3>静态代理 vs 动态代理</h3>
 * <pre>
 *   ┌───────────────────────────────────────────────────────────┐
 *   │                                                           │
 *   │   静态代理（本示例）：                                      │
 *   │   手写一个 UserServiceProxy implements UserService         │
 *   │   ─ 优点：简单直观，适合学习                                │
 *   │   ─ 缺点：每个接口都要手写一个代理类                        │
 *   │                                                           │
 *   │   动态代理（框架使用）：                                     │
 *   │   ServiceProxyFactory.getProxy(UserService.class)         │
 *   │   ─ 优点：一个工厂搞定所有接口                              │
 *   │   ─ 缺点：理解门槛稍高                                     │
 *   │                                                           │
 *   └───────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 *   // 静态代理方式（本示例）
 *   UserService userService = new UserServiceProxy();
 *
 *   // 动态代理方式（注释中的代码）
 *   // UserService userService = ServiceProxyFactory.getProxy(UserService.class);
 *
 *   // 两种方式的调用方式完全一样！
 *   User result = userService.getUser(user);
 * }</pre>
 *
 * @author HGL
 * @see UserServiceProxy —— 手写的静态代理类
 * @see ConsumerExample —— 使用动态代理的完整版本
 */
public class EasyConsumerExample {

    public static void main(String[] args) {
        // 创建代理对象 —— 这里是"手写静态代理"
        // UserServiceProxy 底层会把 getUser() 调用转换成 HTTP 请求
        // 如果想体验动态代理，注释掉下面这行，取消注释下一行
        UserService userService = new UserServiceProxy();
        // 动态代理（需要先调用 ConsumerBootstrap.init()）
        // UserService userService = ServiceProxyFactory.getProxy(UserService.class);

        // 构造请求参数
        User user = new User();
        user.setName("hgl");

        // 调用远程服务 —— 代码看起来和调用本地方法一模一样
        // 但实际上 UserServiceProxy 在幕后完成了：
        //   1. 构造 RpcRequest（服务名、方法名、参数类型、参数值）
        //   2. 使用 JdkSerializer 序列化
        //   3. 通过 Hutool HttpRequest 发送 POST 请求到 localhost:8080
        //   4. 接收响应并反序列化
        User newUser = userService.getUser(user);
        if (newUser != null) {
            System.out.println(newUser.getName());
        } else {
            System.out.println("user == null");
        }
    }
}
