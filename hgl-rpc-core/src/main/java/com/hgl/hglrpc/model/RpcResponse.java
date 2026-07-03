package com.hgl.hglrpc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * RPC 响应对象 —— "远程调用的回执单"
 *
 * <p>继续银行的比喻：你寄出了"委托办理单"（RpcRequest），
 * 银行办完业务后会寄回一张"回执单"，上面写明：
 *   1. 办理结果（data —— 响应数据，比如查到的用户信息）
 *   2. 结果是什么类型（dataType —— 响应数据类型）
 *   3. 办理状态说明（message —— "ok" 表示成功，或具体的错误信息）
 *   4. 如果办理失败，失败原因是什么（exception —— 异常信息）
 *
 * <p>这就是 RpcResponse。它和 RpcRequest 是"一对"——一问一答，
 * 构成了 RPC 通信的基本单元。
 *
 * <p>响应处理流程：
 * <pre>
 *   提供者端                                  消费者端
 *   ┌──────────┐    网络传输（TCP/HTTP）    ┌──────────┐
 *   │ 构造      │ ──── 序列化 ──────────→ │ 反序列化  │
 *   │ RpcResponse│                         │ RpcResponse│
 *   └──────────┘                           └────┬─────┘
 *                                                │
 *                                     ┌─────────▼──────────┐
 *                                     │ 检查 exception      │
 *                                     │ 取出 data 返回给调用者│
 *                                     └────────────────────┘
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/8/29 16:20
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RpcResponse implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 响应数据 —— 远程方法调用的返回值
     *
     * <p>比如调用 getUser() 方法返回的 User 对象。
     * 服务端通过 method.invoke() 得到返回值后，塞进这个字段。
     * 消费者收到响应后，通过 dataType 将其转换为正确的类型使用。
     *
     * <p>注意：这个字段的类型是 Object，因为任何 Java 对象都可能作为返回值。
     */
    private Object data;

    /**
     * 响应数据类型 —— 告诉消费者 data 是什么类型
     *
     * <p>例如：User.class、String.class 等。
     * 这个字段是"导航仪"——没有它，消费者拿到一个 Object，
     * 不知道该把它转成什么类型来使用。
     *
     * <p>当前版本中，消费者通过 Proxy 层已经知道期望的返回类型，
     * 所以这个字段更多是作为"校验"和"兜底"使用。
     */
    private Class<?> dataType;

    /**
     * 响应信息 —— 人可读的状态描述
     *
     * <p>成功时："ok"
     * 失败时：具体的错误信息，如 "未找到服务: xxx"、"方法调用失败"
     *
     * <p>这就像快递物流的状态更新——"已签收"或"派送失败：地址不存在"。
     */
    private String message;

    /**
     * 异常信息 —— 如果远程调用出错，异常会被封装在这里
     *
     * <p>当服务端执行 method.invoke() 抛出异常时，
     * 框架会捕获这个异常并塞进 exception 字段，
     * 然后通过网络传回给消费者。
     *
     * <p>消费者端的 Proxy 会检查这个字段：
     * 如果不为空，说明远程调用失败，可以据此做重试或降级处理。
     *
     * <p>注意：并非所有异常都可序列化。如果异常类没有实现 Serializable，
     * 序列化时可能会丢失部分信息。
     */
    private Exception exception;
}
