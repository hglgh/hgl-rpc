package com.hgl.example.consumer;

import com.hgl.example.common.model.User;
import com.hgl.example.common.service.UserService;
import com.hgl.hglrpc.bootstrap.ConsumerBootstrap;
import com.hgl.hglrpc.proxy.ServiceProxyFactory;

/**
 * RPC 服务消费者的启动入口 —— "打电话订外卖"
 *
 * <p>消费者是 RPC 调用的发起方，就像你拿起手机点外卖一样：
 * <pre>
 *   ┌────────────────────────────────────────────────────────────┐
 *   │                    ConsumerExample                         │
 *   │                                                            │
 *   │  1. ConsumerBootstrap.init() 初始化（连接注册中心）           │
 *   │          ↓                                                 │
 *   │  2. ServiceProxyFactory.getProxy() 获取代理对象             │
 *   │          ↓                                                 │
 *   │  3. 像调用本地方法一样调用远程服务                             │
 *   │     userService.getUser(user)                              │
 *   │          ↓                                                 │
 *   │  4. 代理偷偷帮你完成：                                       │
 *   │     序列化 → 发送网络请求 → 等待响应 → 反序列化 → 返回结果    │
 *   └────────────────────────────────────────────────────────────┘
 * </pre>
 * </p>
 *
 * <h3>核心概念：透明代理</h3>
 * <p>消费者拿到的 {@code UserService} 看起来是一个普通对象，但实际上是一个"代理人"。
 * 每次调用它的方法，代理都在幕后把请求打包、通过网络发送给远程提供者。
 * 这就是 RPC 的精髓 —— <b>让远程调用像本地调用一样简单</b>。</p>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 *   // 1. 初始化（连接注册中心，加载配置等）
 *   ConsumerBootstrap.init();
 *
 *   // 2. 获取代理 —— 框架会自动寻找服务提供者
 *   UserService userService = ServiceProxyFactory.getProxy(UserService.class);
 *
 *   // 3. 和调用本地方法一样使用！
 *   User result = userService.getUser(user);
 * }</pre>
 *
 * @author HGL
 * @see com.hgl.hglrpc.bootstrap.ConsumerBootstrap —— 消费者引导类
 * @see com.hgl.hglrpc.proxy.ServiceProxyFactory —— 代理工厂
 * @see EasyConsumerExample —— 更简易的版本（手动创建代理）
 */
public class ConsumerExample {

    public static void main(String[] args) {
        // ========== 第一步：消费者初始化 ==========
        // ConsumerBootstrap.init() 会：
        //   1) 读取 RpcConfig 配置（注册中心地址、序列化器等）
        //   2) 建立与注册中心的连接，以便后续查找服务提供者
        ConsumerBootstrap.init();

        // ========== 第二步：获取服务代理 ==========
        // ServiceProxyFactory.getProxy() 做的事情：
        //   1) 创建一个实现了 UserService 接口的动态代理对象
        //   2) 代理对象的 invoke 方法会：
        //      - 把方法名、参数类型、参数值封装成 RpcRequest
        //      - 通过注册中心找到提供者地址
        //      - 序列化 RpcRequest 并发送 HTTP/TCP 请求
        //      - 等待响应、反序列化得到 RpcResponse
        //      - 返回结果给调用者
        UserService userService = ServiceProxyFactory.getProxy(UserService.class);

        // ========== 第三步：构建请求并调用 ==========
        User user = new User();
        user.setName("hgl");

        // 循环 10 次调用，可以观察：
        //   1) 负载均衡是否生效（提供者端打印不同端口）
        //   2) 容错机制是否正常（如果有重试策略）
        for (int i = 0; i < 10; i++) {
            User newUser = userService.getUser(user);
            if (newUser != null) {
                System.out.println(newUser.getName());
            } else {
                System.out.println("user == null");
            }
            // 以下代码可用于测试无参远程调用：
            // long number = userService.getNumber();
            // System.out.println(number);
        }
    }
}
