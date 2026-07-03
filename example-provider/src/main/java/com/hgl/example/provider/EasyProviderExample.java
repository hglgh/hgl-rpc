package com.hgl.example.provider;

import com.hgl.example.common.service.UserService;
import com.hgl.hglrpc.registry.LocalRegistry;
import com.hgl.hglrpc.server.http.VertxHttpServer;

/**
 * 简易版提供者示例 —— "手动挡"的启动方式
 *
 * <p>与 {@link ProviderBootstrap} 的"自动挡"不同，这里每个步骤都手动完成，
 * 帮助你理解框架底层到底做了什么。</p>
 *
 * <h3>手动挡 vs 自动挡</h3>
 * <pre>
 *   ┌─────────────────────────────────────────────────────────────┐
 *   │  步骤            │  EasyProviderExample (手动挡)             │
 *   │                  │  ProviderExample (自动挡)                 │
 *   ├──────────────────┼─────────────────────────────────────────┤
 *   │ 1. 注册服务       │ LocalRegistry.register() 手动注册实例     │
 *   │                  │ ProviderBootstrap.init() 自动注册         │
 *   ├──────────────────┼─────────────────────────────────────────┤
 *   │ 2. 启动服务器     │ new VertxHttpServer().doStart() 手动启动  │
 *   │                  │ ProviderBootstrap 自动启动                │
 *   ├──────────────────┼─────────────────────────────────────────┤
 *   │ 3. 注册到注册中心  │ 不涉及（纯本地）                          │
 *   │                  │ 自动注册到 Etcd 等注册中心                  │
 *   └─────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>适用场景</h3>
 * <p>适合学习框架原理、快速原型验证、或不需要注册中心的单机调试场景。</p>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 *   // 第一步：把接口名和实现实例的映射关系注册到本地注册表
 *   LocalRegistry.register(UserService.class.getName(), new UserServiceImpl());
 *
 *   // 第二步：启动 HTTP 服务器，监听 8080 端口
 *   new VertxHttpServer().doStart(8080);
 * }</pre>
 *
 * @author HGL
 * @see ProviderExample —— 更完整的"自动挡"版本
 * @see com.hgl.hglrpc.registry.LocalRegistry —— 本地服务注册表
 */
public class EasyProviderExample {

    public static void main(String[] args) {
        // 第一步：注册服务到本地注册表
        // 注意：这里注册的是"实例"（new UserServiceImpl()），而非 Class
        // 框架收到请求后，直接从 LocalRegistry 中取出实例并反射调用
        // 这和 ProviderExample 注册 Class 的方式不同（ProviderExample 会用反射创建新实例）
        LocalRegistry.register(UserService.class.getName(), new UserServiceImpl());

        // 第二步：启动 Vertx HTTP 服务器，监听 8080 端口
        // 服务器启动后，消费者就可以通过 http://localhost:8080 发送 RPC 请求了
        new VertxHttpServer().doStart(8080);
    }
}
