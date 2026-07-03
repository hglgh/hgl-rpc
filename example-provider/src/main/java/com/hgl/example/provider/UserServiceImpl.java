package com.hgl.example.provider;

import com.hgl.example.common.model.User;
import com.hgl.example.common.service.UserService;
import com.hgl.hglrpc.RpcApplication;

/**
 * 用户服务的实现 —— "厨师真正动手做菜"
 *
 * <p>如果说 {@link com.hgl.example.common.service.UserService} 是菜单（接口），
 * 那这个类就是厨师（实现）——消费者点什么菜，厨师就做什么菜。</p>
 *
 * <h3>在 RPC 调用链中的位置</h3>
 * <pre>
 *   消费者                    网络                   提供者
 *   ──────                ────────                ────────
 *   userService
 *     .getUser(user)
 *         │                                      ┌──────────────┐
 *         ▼                                      │ UserServiceImpl│
 *   [序列化]  ──── HTTP/TCP ────▶  [反序列化] ──▶ │  .getUser()   │
 *                                                  │  打印信息      │
 *                                                  │  返回 user    │
 *                                                  └──────────────┘
 *                                                        │
 *                                            [序列化] ◀───┘
 *                                    ◀── HTTP/TCP ────
 *   [反序列化]
 *       │
 *       ▼
 *   得到结果
 * </pre>
 *
 * <h3>小技巧</h3>
 * <p>打印服务端口 {@code RpcApplication.getRpcConfig().getServerPort()} 可以帮助验证
 * 负载均衡是否生效 —— 如果多次调用打印了不同端口，说明请求被分发到了不同的提供者实例。</p>
 *
 * @author HGL
 * @see com.hgl.example.common.service.UserService —— 本类实现的接口
 * @see com.hgl.example.provider.ProviderExample —— 注册此实现的启动类
 */
public class UserServiceImpl implements UserService {

    /**
     * 处理用户请求 —— 提供者侧的核心业务逻辑
     *
     * <p>当消费者调用 {@code userService.getUser(user)} 时，框架通过反射调用此方法。
     * 这里演示了一个简单场景：打印用户名和当前服务器端口，然后原样返回。</p>
     *
     * @param user 消费者传来的用户对象（经过网络传输、反序列化后的副本）
     * @return 处理后的用户对象（会被序列化后发回给消费者）
     */
    @Override
    public User getUser(User user) {
        // 打印调用信息：用户名 + 当前实例的端口号
        // 端口号在负载均衡场景下非常有用，可以验证请求是否均匀分发
        System.out.println("用户名：" + user.getName() + "，处理实例端口：" + RpcApplication.getRpcConfig().getServerPort());
        return user;
    }
}
