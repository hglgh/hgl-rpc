package com.hgl.hglrpc.proxy;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * 模拟的代理人（MockServiceProxy）
 *
 * <p>这个代理人和真正的代理人（ServiceProxy）长得一模一样，但他其实是个"占位的"。
 * 他收到你的信后，不会真的帮你寄出去——他会根据你期望的回信格式，
 * 随手编一个默认值交还给你。</p>
 *
 * <p>什么时候会用到他？</p>
 * <ul>
 *   <li><b>本地开发调试</b>：服务提供者还没部署，消费者先跑起来测测界面逻辑</li>
 *   <li><b>单元测试</b>：不想依赖真实的服务端，只测消费者自身的逻辑</li>
 *   <li><b>快速失败验证</b>：检查代理链路是否通畅，不必等真实的网络调用</li>
 * </ul>
 *
 * <p>他的行为非常简单：不管调什么方法，都返回一个"安全的空值"。
 * 就像你去邮局问"有没有我的信？"，邮局大叔说"没有"，而不是报错。</p>
 *
 * <pre>
 *   ┌──────────┐      ┌─────────────────┐      ┌──────────────┐
 *   │ 业务代码  │ ──> │ MockServiceProxy │ ──> │  返回默认值    │
 *   │ 调用接口  │      │ (不真的发请求)    │      │  (null/0等)   │
 *   └──────────┘      └─────────────────┘      └──────────────┘
 *
 *   对比真正的代理：
 *   ┌──────────┐      ┌─────────────────┐      ┌──────────────┐
 *   │ 业务代码  │ ──> │  ServiceProxy   │ ──> │  网络调用远程  │
 *   │ 调用接口  │      │ (真的发请求)      │      │  服务并返回   │
 *   └──────────┘      └─────────────────┘      └──────────────┘
 * </pre>
 *
 * @author HGL
 * @since 2025/9/1 14:28
 */
@Slf4j
public class MockServiceProxy implements InvocationHandler {

    /**
     * 代理调用的入口——不管调什么方法，都返回一个无害的默认值。
     *
     * <p>为什么根据返回类型来生成默认值？因为 Java 是强类型语言，
     * 如果方法声明返回 int，你不能返回 null，否则会 NullPointerException。
     * 所以我们需要为每种基本类型返回对应的"零值"，对象类型则可以安全地返回 null。</p>
     *
     * @param proxy  代理对象本身
     * @param method 被调用的方法
     * @param args   方法参数（在这里完全被忽略，因为不执行真正的逻辑）
     * @return 根据方法返回类型生成的默认值
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        // 获取方法的返回值类型，用于决定返回什么默认值
        Class<?> methodReturnType = method.getReturnType();
        log.info("mock invoke {}", method.getName());
        return getDefaultObject(methodReturnType);
    }

    /**
     * 根据类型生成安全的默认值——不会抛异常的那种。
     *
     * <p>这个方法就像一个"万能填空器"：你告诉它要填什么类型的空，
     * 它就给你一个不会出错的值。基本类型填"零"，对象类型填 null。</p>
     *
     * <p>注意：这里只处理了常见的基本类型。
     * 如果你需要更完善的默认值逻辑（比如返回空集合、空字符串等），
     * 可以根据业务需要扩展这个方法。</p>
     *
     * @param type 需要生成默认值的类型
     * @return 该类型对应的安全默认值
     */
    private Object getDefaultObject(Class<?> type) {
        // ===== 基本类型：必须返回对应的零值，不能返回 null =====
        if (type.isPrimitive()) {
            if (type == boolean.class) {
                return false;           // boolean 的零值是 false
            } else if (type == short.class) {
                return (short) 0;       // 整型家族的零值都是 0
            } else if (type == int.class) {
                return 0;
            } else if (type == long.class) {
                return 0L;
            }
            // 其他基本类型（byte、char、float、double）默认由 JVM 提供零值
            // 这里未覆盖的情况通常不会出现在 RPC 接口中
        }
        // ===== 对象类型：null 是安全的——调用方自己做空判断即可 =====
        return null;
    }
}
