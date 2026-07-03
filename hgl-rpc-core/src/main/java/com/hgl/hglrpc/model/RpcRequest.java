package com.hgl.hglrpc.model;

import com.hgl.hglrpc.constant.RpcConstant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * RPC 请求对象 —— "远程调用的快递单"
 *
 * <p>想象一下：你要去银行办理业务，但银行在另一个城市。
 * 你不能亲自过去，于是填了一张"委托办理单"，写清楚：
 *   1. 去哪家银行（serviceName —— 服务名称）
 *   2. 办什么业务（methodName —— 方法名称）
 *   3. 带什么材料（args —— 参数列表）
 *   4. 材料是什么类型的（parameterTypes —— 参数类型列表）
 *
 * <p>这张"委托单"就是 RpcRequest。它会被序列化成字节流，
 * 通过网络发送到远程服务器，服务器收到后按照单子上的信息
 * 找到对应的服务、调用对应的方法、传入对应的参数，最后把结果返回给你。
 *
 * <p>工作流程：
 * <pre>
 *   消费者端                                  提供者端
 *   ┌──────────┐    网络传输（TCP/HTTP）    ┌──────────┐
 *   │ 构造      │ ──── 序列化 ──────────→ │ 反序列化  │
 *   │ RpcRequest│                          │ RpcRequest│
 *   └──────────┘                           └────┬─────┘
 *                                                │
 *                                     ┌─────────▼──────────┐
 *                                     │ 反射调用目标方法      │
 *                                     │ method.invoke(obj, args) │
 *                                     └────────────────────┘
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/8/29 16:10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 服务名称（接口的全限定类名）
     *
     * <p>例如："com.hgl.example.common.service.UserService"
     * 这相当于告诉远程服务器："我要调用的是 UserService 这个服务"。
     * 服务端会根据这个名字在本地注册表（LocalRegistry）中查找对应的实现类。
     */
    private String serviceName;

    /**
     * 方法名称
     *
     * <p>例如："getUser"
     * 通过反射机制，服务端会找到实现类中与这个名称匹配的方法来调用。
     * 这就是 RPC 的核心魔法——你调用的是接口方法，执行的却是远端的实现。
     */
    private String methodName;

    /**
     * 服务版本号
     *
     * <p>默认值为 "1.0"（由 RpcConstant.DEFAULT_SERVICE_VERSION 定义）。
     * 版本号用于灰度发布和服务多版本共存的场景。
     * 比如：同一个 UserService，v1.0 和 v2.0 可以同时注册在注册中心，
     * 消费者通过指定版本号来选择调用哪个版本。
     */
    private String serviceVersion = RpcConstant.DEFAULT_SERVICE_VERSION;

    /**
     * 参数类型列表
     *
     * <p>例如：new Class[]{User.class, String.class}
     * 这是 Java 反射的必要信息——同名方法可能有多个重载版本，
     * 只有通过参数类型才能精确定位到唯一的方法。
     * <p>
     * 注意：这里用 Class<?>[] 而非 Class[]，泛型擦除后序列化/反序列化需要特殊处理。
     */
    private Class<?>[] parameterTypes;

    /**
     * 参数列表（实际传入的参数值）
     *
     * <p>例如：new Object[]{user, "hello"}
     * 与 parameterTypes 一一对应，是调用方法时传入的真实参数。
     * 这些参数会被序列化后通过网络传输，所以参数类型必须是可序列化的。
     */
    private Object[] args;
}
