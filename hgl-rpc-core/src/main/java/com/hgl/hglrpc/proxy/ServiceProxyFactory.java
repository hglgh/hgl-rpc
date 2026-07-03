package com.hgl.hglrpc.proxy;

import com.hgl.hglrpc.RpcApplication;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Proxy;

/**
 * 代理人工厂（ServiceProxyFactory）
 *
 * <p>如果 ServiceProxy 是一个专业的代理人，MockServiceProxy 是一个临时占位的代理人，
 * 那么 ServiceProxyFactory 就是"代理人派遣中心"。</p>
 *
 * <p>你不需要自己去找代理人、签合同、交代需求——你只需要告诉派遣中心
 * "我要一个能代理 UserService 的人"，它就帮你安排好。</p>
 *
 * <p>更妙的是，派遣中心还会根据你的"配置偏好"来派不同的人：</p>
 * <ul>
 *   <li><b>正常模式</b>：派出真正的代理人（ServiceProxy），帮你走完整个远程调用流程</li>
 *   <li><b>Mock 模式</b>：派出模拟代理人（MockServiceProxy），不发真实请求，返回默认值</li>
 * </ul>
 *
 * <p>底层依赖 JDK 动态代理机制（{@link Proxy#newProxyInstance}），
 * 在运行时动态生成一个实现了目标接口的代理对象。
 * 对调用方来说，拿到的代理对象和真正的服务实现长得一模一样——
 * 你调用 userService.getUserById(1L)，完全不知道背后走的是本地方法还是网络请求。</p>
 *
 * <pre>
 *   调用方视角（看到的）：                   实际发生的：
 *   ┌──────────────────┐               ┌──────────────────────────┐
 *   │ UserService svc  │               │ JDK 动态代理对象           │
 *   │ svc.getUser(1L)  │  ── 神奇的 ──> │ 实际调用 ServiceProxy     │
 *   │ // 返回 User     │    隐形转换    │ .invoke(proxy, method,   │
 *   └──────────────────┘               │         args)            │
 *                                      └──────────────────────────┘
 * </pre>
 *
 * @author HGL
 * @since 2025/8/29 17:12
 */
@Slf4j
public class ServiceProxyFactory {

    /**
     * 根据服务接口创建代理对象——你给我一个接口，我给你一个能远程调用的"替身"。
     *
     * <p>这个方法是消费者使用 RPC 框架的最常见入口之一。
     * 典型用法：</p>
     * <pre>
     *   // 一行代码，拿到远程服务的代理对象
     *   UserService userService = ServiceProxyFactory.getProxy(UserService.class);
     *
     *   // 像调用本地方法一样调用远程服务——魔法！
     *   User user = userService.getUserById(1L);
     * </pre>
     *
     * <p>为什么用泛型？为了让调用方拿到的直接就是目标类型的对象，
     * 不需要手动强转，既安全又方便。</p>
     *
     * @param serviceClass 服务接口的 Class 对象（如 UserService.class）
     * @param <T>          服务接口类型
     * @return 实现了该接口的代理对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T getProxy(Class<T> serviceClass) {
        // 如果开启了 Mock 模式，就不走真实的远程调用了
        // 适合本地开发、测试阶段，不依赖服务端即可跑通调用链路
        if (RpcApplication.getRpcConfig().isMock()) {
            return getMockProxy(serviceClass);
        }

        // 创建 JDK 动态代理：三个参数分别是
        // 1. classLoader —— 用目标接口的类加载器，保证类型一致性
        // 2. interfaces —— 代理要实现的接口列表（这里只有一个）
        // 3. handler    —— 核心！方法调用时实际执行的逻辑（ServiceProxy）
        return (T) Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class[]{serviceClass},
                new ServiceProxy());
    }

    /**
     * 创建 Mock 代理对象——不发真实请求，只返回安全的默认值。
     *
     * <p>当你只想测试消费者侧的代码逻辑（比如参数封装、结果处理），
     * 而不想等待服务端就绪时，Mock 代理就派上用场了。
     * 它就像一个"假人模特"——样子和真人一样，但不会真的回答你。</p>
     *
     * @param serviceClass 服务接口的 Class 对象
     * @param <T>          服务接口类型
     * @return Mock 代理对象（调用任何方法都返回默认值）
     */
    @SuppressWarnings("unchecked")
    public static <T> T getMockProxy(Class<T> serviceClass) {
        log.info("MockServiceProxy的类加载器: {}", serviceClass.getClassLoader());
        return (T) Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class[]{serviceClass},
                new MockServiceProxy());
    }
}
