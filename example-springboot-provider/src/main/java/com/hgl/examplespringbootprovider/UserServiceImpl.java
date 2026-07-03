package com.hgl.examplespringbootprovider;

import com.hgl.example.common.model.User;
import com.hgl.example.common.service.UserService;
import com.hgl.hglrpcspringbootstarter.annotation.RpcService;

/**
 * Spring Boot 风格的用户服务实现 —— "贴上标签，自动上岗"
 *
 * <p>与原生 API 版本的 {@link com.hgl.example.provider.UserServiceImpl} 相比，
 * 这里只需要加一个 {@code @RpcService} 注解，就完成了服务注册的全部工作。</p>
 *
 * <h3>{@code @RpcService} 注解的魔力</h3>
 * <pre>
 *   ┌──────────────────────────────────────────────────────────────┐
 *   │  Spring 容器启动时：                                          │
 *   │                                                              │
 *   │  1. @EnableRpc 触发自定义扫描                                 │
 *   │          ↓                                                   │
 *   │  2. 发现 UserServiceImpl 上有 @RpcService                     │
 *   │          ↓                                                   │
 *   │  3. 自动将此 Bean 注册为 RPC 服务                              │
 *   │     (等效于手动调用 LocalRegistry.register())                  │
 *   │          ↓                                                   │
 *   │  4. 框架启动 HTTP/TCP 服务器，等待消费者调用                    │
 *   └──────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <h3>与原生版的区别</h3>
 * <table>
 *   <tr><th>对比项</th><th>原生版</th><th>Spring Boot 版</th></tr>
 *   <tr><td>注册方式</td><td>手动调用 LocalRegistry.register()</td><td>加 @RpcService 注解即可</td></tr>
 *   <tr><td>实例管理</td><td>手动 new 或注册 Class</td><td>Spring 容器管理（自动注入、AOP 等）</td></tr>
 *   <tr><td>配置方式</td><td>代码硬编码或 RpcConfig</td><td>application.yml 统一配置</td></tr>
 * </table>
 *
 * @author HGL
 * @see com.hgl.hglrpcspringbootstarter.annotation.RpcService —— RPC 服务注册注解
 * @see com.hgl.example.common.service.UserService —— 本类实现的接口
 */
@RpcService
public class UserServiceImpl implements UserService {

    /**
     * 处理用户请求 —— 与原生版逻辑相同
     *
     * <p>框架会自动把这个 Bean 注册为 RPC 服务。
     * 消费者通过 {@code UserService} 接口调用时，请求最终会到达这个方法。</p>
     *
     * @param user 消费者传来的用户对象
     * @return 处理后的用户对象
     */
    @Override
    public User getUser(User user) {
        // 注意：这里没有像原生版那样打印端口号
        // 因为 Spring Boot 版不需要通过 RpcApplication.getRpcConfig() 获取配置
        System.out.println("用户名：" + user.getName());
        return user;
    }
}
