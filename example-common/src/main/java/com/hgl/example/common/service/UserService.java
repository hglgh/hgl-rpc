package com.hgl.example.common.service;

import com.hgl.example.common.model.User;

/**
 * 用户服务接口 —— 提供者和消费者之间的"契约"
 *
 * <h3>这个接口为什么放在 common 模块？</h3>
 * <p>在 RPC 架构中，接口就是"合同"：
 * <pre>
 *   ┌──────────────┐         网络          ┌──────────────┐
 *   │   消费者       │  ──────────────────▶  │   提供者       │
 *   │ (调用接口方法)  │                       │ (实现接口方法)  │
 *   └──────────────┘                        └──────────────┘
 *          │                                        │
 *          └────── 都依赖 common 模块中的这个接口 ──────┘
 * </pre>
 * </p>
 *
 * <h3>为什么要共享接口？</h3>
 * <ul>
 *   <li><b>消费者</b>只依赖接口编程，不关心实现细节（面向接口编程）</li>
 *   <li><b>提供者</b>实现这个接口，提供真正的业务逻辑</li>
 *   <li><b>框架</b>通过接口名在注册中心查找服务，把调用转发给真正的实现</li>
 * </ul>
 *
 * <h3>使用方式</h3>
 * <pre>{@code
 *   // 消费者侧 —— 通过代理调用（像调用本地方法一样）
 *   UserService userService = ServiceProxyFactory.getProxy(UserService.class);
 *   User result = userService.getUser(new User());
 *
 *   // 提供者侧 —— 实现此接口
 *   public class UserServiceImpl implements UserService { ... }
 * }</pre>
 *
 * @author HGL
 * @see com.hgl.example.common.model.User —— 接口使用的核心模型
 */
public interface UserService {

    /**
     * 获取用户信息 —— RPC 远程调用的核心方法
     *
     * <p>消费者调用此方法时，框架会：</p>
     * <ol>
     *   <li>把方法名、参数类型、参数值打包成 {@link com.hgl.hglrpc.model.RpcRequest}</li>
     *   <li>序列化后通过网络发送给提供者</li>
     *   <li>提供者反序列化、反射调用真正的 {@code UserServiceImpl.getUser()}</li>
     *   <li>把返回值打包成 {@link com.hgl.hglrpc.model.RpcResponse} 返回给消费者</li>
     * </ol>
     *
     * @param user 请求用户（携带查询条件）
     * @return 处理后的用户信息
     */
    User getUser(User user);

    /**
     * 获取一个数字 —— 用于测试无参远程调用
     *
     * <p>这是一个 default 方法，不需要提供者强制实现。
     * 可用来验证框架对无参调用的处理能力。</p>
     *
     * @return 固定返回 1
     */
    default short getNumber() {
        return 1;
    }
}
