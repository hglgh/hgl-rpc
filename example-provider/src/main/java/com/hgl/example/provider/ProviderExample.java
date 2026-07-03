package com.hgl.example.provider;

import com.hgl.example.common.service.UserService;
import com.hgl.hglrpc.bootstrap.ProviderBootstrap;
import com.hgl.hglrpc.model.ServiceRegisterInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * RPC 服务提供者的启动入口 —— "开店营业"
 *
 * <p>这个类是提供者端的 main 方法所在，相当于"开门迎客"的第一步：
 * <pre>
 *   ┌─────────────────────────────────────────────────┐
 *   │                 ProviderExample                  │
 *   │                                                  │
 *   │  1. 准备服务清单（哪些服务要对外提供）               │
 *   │          ↓                                       │
 *   │  2. 交给 ProviderBootstrap.init() 初始化          │
 *   │          ↓                                       │
 *   │  3. 框架自动完成：                                 │
 *   │     - 启动 HTTP/TCP 服务器（摆好柜台）              │
 *   │     - 把服务注册到注册中心（挂上招牌）               │
 *   │     - 等待消费者请求（等待顾客上门）                 │
 *   └─────────────────────────────────────────────────┘
 * </pre>
 * </p>
 *
 * <h3>与 EasyProviderExample 的区别</h3>
 * <p>Easy 版本手动注册实例、手动启动服务器；这里通过 {@link ProviderBootstrap} 一站式搞定，
 * 更接近生产环境的用法。</p>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 *   // 1. 准备要注册的服务列表
 *   List<ServiceRegisterInfo<?>> list = new ArrayList<>();
 *   list.add(new ServiceRegisterInfo<>(UserService.class.getName(), UserServiceImpl.class));
 *
 *   // 2. 一行初始化，框架帮你搞定剩下的事
 *   ProviderBootstrap.init(list);
 * }</pre>
 *
 * @author HGL
 * @see com.hgl.hglrpc.bootstrap.ProviderBootstrap —— 提供者引导类
 * @see UserServiceImpl —— 真正的服务实现
 */
public class ProviderExample {

    public static void main(String[] args) {
        // ========== 第一步：准备服务注册清单 ==========
        // ServiceRegisterInfo 就像一张"服务名片"，告诉框架：
        //   - serviceName: 服务接口的全限定名（消费者通过这个名字来找到你）
        //   - implClass:   服务实现类（框架在收到请求后用反射创建实例并调用）
        List<ServiceRegisterInfo<?>> serviceRegisterInfoList = new ArrayList<>();
        ServiceRegisterInfo<UserService> serviceRegisterInfo =
                new ServiceRegisterInfo<>(UserService.class.getName(), UserServiceImpl.class);
        serviceRegisterInfoList.add(serviceRegisterInfo);

        // ========== 第二步：交给框架初始化 ==========
        // ProviderBootstrap.init() 会：
        //   1) 根据 RpcConfig 读取配置（序列化器、端口、注册中心地址等）
        //   2) 启动 VertxHttpServer 或 VertxTcpServer 监听请求
        //   3) 把服务信息注册到注册中心（如 Etcd、Nacos）
        ProviderBootstrap.init(serviceRegisterInfoList);
    }
}
