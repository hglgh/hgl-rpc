package com.hgl.example.consumer.proxy;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.hgl.example.common.model.User;
import com.hgl.example.common.service.UserService;
import com.hgl.example.consumer.EasyConsumerExample;
import com.hgl.hglrpc.model.RpcRequest;
import com.hgl.hglrpc.model.RpcResponse;
import com.hgl.hglrpc.serializer.JdkSerializer;
import lombok.extern.slf4j.Slf4j;

/**
 * 用户服务的手写静态代理 —— "揭开 RPC 的面纱"
 *
 * <p>这个类是理解 RPC 框架原理的钥匙。它展示了当消费者调用 {@code userService.getUser()} 时，
 * 底层到底发生了什么。</p>
 *
 * <h3>调用流程图解</h3>
 * <pre>
 *   消费者代码                       UserServiceProxy                      提供者
 *   ──────────                     ─────────────────                    ──────────
 *   userService.getUser(user)
 *          │
 *          ▼
 *   ┌─────────────────────────────────────────────────────┐
 *   │ 1. 组装 RpcRequest                                   │
 *   │    - serviceName: "UserService"                     │
 *   │    - methodName: "getUser"                          │
 *   │    - parameterTypes: [User.class]                   │
 *   │    - args: [user]                                   │
 *   └─────────────────┬───────────────────────────────────┘
 *                     │
 *                     ▼
 *   ┌─────────────────────────────────────────────────────┐
 *   │ 2. JdkSerializer 序列化                              │
 *   │    RpcRequest → byte[]                              │
 *   └─────────────────┬───────────────────────────────────┘
 *                     │
 *                     ▼
 *   ┌─────────────────────────────────────────────────────┐
 *   │ 3. HTTP POST 发送到 http://localhost:8080            │
 *   │    body = byte[] (序列化后的请求)                      │
 *   └─────────────────┬───────────────────────────────────┘
 *                     │                         ┌──────────────────┐
 *                     └───────────────────────▶ │   提供者处理请求    │
 *                                               │   返回 byte[]     │
 *                                               └────────┬─────────┘
 *                                                        │
 *   ┌────────────────────────────────────────────────────┘┐
 *   │ 4. 反序列化响应                                        │
 *   │    byte[] → RpcResponse → User                      │
 *   └─────────────────┬───────────────────────────────────┘
 *                     │
 *                     ▼
 *             返回 User 对象给调用者
 * </pre>
 *
 * <h3>为什么是"静态"代理？</h3>
 * <p>因为这个类是手写的、只服务于 {@link UserService} 这一个接口。
 * 框架实际使用的是 JDK 动态代理（{@link java.lang.reflect.Proxy}），
 * 可以用一个通用的 InvocationHandler 处理所有接口的调用，不需要为每个接口写一个代理类。</p>
 *
 * <h3>学习价值</h3>
 * <p>读完这个类，你就理解了 RPC 框架最核心的原理：
 * <b>代理 + 序列化 + 网络传输 = 远程调用</b></p>
 *
 * @author HGL
 * @see com.hgl.hglrpc.proxy.ServiceProxyFactory —— 框架的动态代理工厂
 * @see EasyConsumerExample —— 使用此代理的示例
 */
@Slf4j
public class UserServiceProxy implements UserService {

    /**
     * 代理的 getUser 方法 —— 把本地调用"偷天换日"成远程调用
     *
     * <p>这个方法的签名和 {@link UserService#getUser(User)} 一模一样，
     * 但内部做的事情完全不同：不是处理业务，而是构造网络请求。</p>
     *
     * @param user 消费者传入的用户参数
     * @return 从远程提供者返回的用户对象
     */
    @Override
    public User getUser(User user) {
        // ========== 第一步：选择序列化器 ==========
        // JdkSerializer 使用 Java 原生序列化，简单但性能一般
        // 生产环境建议用 JsonSerializer（可读性好）或 HessianSerializer（性能好）
        JdkSerializer jdkSerializer = new JdkSerializer();

        // ========== 第二步：构造 RPC 请求对象 ==========
        // RpcRequest 就是"快递单"，包含：
        //   - serviceName:     接口全限定名（"寄给谁"）
        //   - methodName:      方法名（"做什么事"）
        //   - parameterTypes:  参数类型数组（"用什么方式做"）
        //   - args:            参数值数组（"具体内容"）
        RpcRequest rpcRequest = RpcRequest.builder()
                .serviceName(UserService.class.getName())
                .methodName("getUser")
                .parameterTypes(new Class[]{User.class})
                .args(new Object[]{user})
                .build();

        try {
            // ========== 第三步：序列化 + 发送 HTTP 请求 ==========
            // serialize: 把 RpcRequest 对象 → byte[] 字节流
            byte[] bytes = jdkSerializer.serialize(rpcRequest);

            // 使用 Hutool 的 HttpRequest 发送 POST 请求
            //   - URL 指向提供者服务器地址（硬编码 localhost:8080）
            //   - body 是序列化后的字节流
            //   - 生产环境中 URL 应该从注册中心动态获取
            byte[] result;
            try (HttpResponse httpResponse = HttpRequest.post("http://localhost:8080")
                    .body(bytes)
                    .execute()) {
                result = httpResponse.bodyBytes();
            }

            // ========== 第四步：反序列化响应 ==========
            // 把提供者返回的 byte[] → RpcResponse 对象
            // 再从 RpcResponse 中取出 data 字段，强转为 User
            RpcResponse rpcResponse = jdkSerializer.deserialize(result, RpcResponse.class);
            return (User) rpcResponse.getData();
        } catch (Exception e) {
            log.error("Error: ", e);
        }
        return null;
    }
}
