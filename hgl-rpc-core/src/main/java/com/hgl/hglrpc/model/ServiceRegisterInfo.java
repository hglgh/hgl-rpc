package com.hgl.hglrpc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 服务注册信息 —— "服务提供者的注册申请表"
 *
 * <p>当框架启动时，需要知道"我应该对外提供哪些服务"。
 * ServiceRegisterInfo 就是这张"申请表"，告诉框架：
 *   - 我要提供什么服务（serviceName —— 接口名）
 *   - 用什么实现类来提供（implClass —— 实现类的 Class 对象）
 *
 * <p>使用场景：
 * <pre>
 *   // 在 ProviderBootstrap 中，用户声明自己要注册的服务：
 *   ServiceRegisterInfo&lt;UserService&gt; info = new ServiceRegisterInfo&lt;&gt;(
 *       UserService.class.getName(),  // 接口名作为服务名
 *       UserServiceImpl.class         // 实现类
 *   );
 *
 *   // 框架拿到这个信息后会：
 *   // 1. 用 implClass 反射创建实例
 *   // 2. 把 实例 存入 LocalRegistry（本地注册表）
 *   // 3. 把 ServiceMetaInfo 存入注册中心（Etcd/ZooKeeper）
 * </pre>
 *
 * <p>泛型 T 的作用：约束 implClass 必须是 T 的子类，
 * 这样在编译期就能发现接口和实现类不匹配的错误。
 * 比如 `ServiceRegisterInfo<UserService>` 的 implClass 必须是 `Class<? extends UserService>`。
 *
 * @param <T> 服务接口类型
 * @Author HGL
 * @Create: 2025/9/5 15:43
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceRegisterInfo<T> {
    /**
     * 服务名称（通常就是接口的全限定类名）
     *
     * <p>例如："com.hgl.example.common.service.UserService"
     * 这个名字会同时出现在：
     *   - 本地注册表（LocalRegistry）中，作为查找服务实例的 key
     *   - 注册中心中，作为服务发现的 key
     */
    private String serviceName;

    /**
     * 服务实现类的 Class 对象
     *
     * <p>例如：UserServiceImpl.class
     * 框架会在启动时通过反射创建这个类的实例：
     * {@code implClass.getDeclaredConstructor().newInstance()}
     *
     * <p>注意：实现类必须有无参构造函数，否则反射创建会失败。
     * 如果使用 Spring Boot Starter，这里会直接注入 Spring 容器中的 Bean，
     * 而不是反射创建，所以可以使用构造器注入等高级特性。
     */
    private Class<? extends T> implClass;
}
