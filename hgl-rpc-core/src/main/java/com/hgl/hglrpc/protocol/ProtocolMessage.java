package com.hgl.hglrpc.protocol;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 协议消息结构 —— "网络传输的信封 + 信纸"
 *
 * <p>ProtocolMessage 是自定义二进制协议的 Java 表示。
 * 每一次 RPC 调用（请求或响应）都会被封装成一个 ProtocolMessage 对象，
 * 然后由 Encoder 编码成字节流发送，由 Decoder 从字节流还原。
 *
 * <p>信封 = Header（消息头）：告诉邮局怎么处理这封信
 * <pre>
 *   ┌──────────────────────────────────────────────────────────┐
 *   │  Header（信封/快递单）                                      │
 *   │  ┌──────────┬─────────────────────────────────────────┐  │
 *   │  │ magic    │ 魔数：协议的"防伪标识"，快速鉴别合法数据        │  │
 *   │  │ version  │ 版本号：协议的"代次"，支持向后兼容             │  │
 *   │  │ serializer│ 序列化器：用什么"语言"写信（JDK/JSON/Kryo）  │  │
 *   │  │ type     │ 类型：是请求信还是回信？                     │  │
 *   │  │ status   │ 状态：处理成功还是失败？                     │  │
 *   │  │ requestId│ 请求ID：唯一的"快递单号"，用于匹配请求和响应    │  │
 *   │  │ bodyLength│ 体长度：信纸有多长，防止读到别人的信          │  │
 *   │  └──────────┴─────────────────────────────────────────┘  │
 *   └──────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>信纸 = Body（消息体）：实际内容，即 RpcRequest 或 RpcResponse 对象
 * <pre>
 *   ┌──────────────────────────────────┐
 *   │  Body（信纸/包裹内容）              │
 *   │  RpcRequest 或 RpcResponse 的     │
 *   │  序列化字节                        │
 *   └──────────────────────────────────┘
 * </pre>
 *
 * @param <T> 消息体类型，通常为 RpcRequest 或 RpcResponse
 * @Author HGL
 * @Create: 2025/9/3 14:52
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProtocolMessage<T> {

    /**
     * 消息头 —— 邮件的"信封"
     *
     * <p>包含协议版本、序列化方式、消息类型等元信息。
     * 编码时会被写成 17 字节的定长头部。
     */
    private Header header;

    /**
     * 消息体 —— 邮件的"内容"
     *
     * <p>泛型 T，通常是 RpcRequest（请求）或 RpcResponse（响应）。
     * 编码时会被序列化成字节数组，追加在头部之后。
     */
    private T body;

    /**
     * 协议消息头 —— "信封上的每一栏"
     *
     * <p>Header 中的每个字段都对应协议报文中的一个固定位置。
     * 字段类型使用 int 而非 byte，因为在 Java 中 byte 是有符号的（-128~127），
     * 如果某个字段的值超过 127，用 byte 比较时会出现符号问题。
     * 编解码时通过 & 0xFF 运算确保正确转换。
     */
    @Data
    public static class Header {
        /**
         * 魔数 —— 协议的"防伪水印"
         *
         * <p>解码器收到数据后，第一个检查的就是魔数。
         * 如果魔数不对，说明这不是合法的 RPC 消息，直接丢弃。
         * 类比：你收到一封"银行来信"，先看有没有银行的印章。
         */
        private int magic;

        /**
         * 版本号 —— 协议的"代次"
         *
         * <p>用于协议升级时的兼容处理。
         * 比如 v1 版本的头部是 17 字节，v2 可能扩展到 21 字节，
         * 解码器先读版本号，再按对应版本的规则解析后续字段。
         */
        private int version;

        /**
         * 序列化器标识 —— 消息体用什么"语言"书写
         *
         * <p>0=JDK, 1=JSON, 2=Kryo, 3=Hessian
         * 解码器需要知道消息体用什么序列化方式，才能正确反序列化。
         * 就像收到一封英文信，你得知道是英式英语还是美式英语（虽然差别不大）。
         */
        private int serializer;

        /**
         * 消息类型 —— 这是"请求"还是"响应"？
         *
         * <p>0=REQUEST（请求）, 1=RESPONSE（响应）, 2=HEART_BEAT（心跳）, 3=OTHERS
         * 服务端收到 REQUEST 后处理业务逻辑返回 RESPONSE；
         * 心跳消息用于保持连接活跃，避免被防火墙或 NAT 设备断开。
         */
        private int type;

        /**
         * 状态码 —— 处理结果的状态
         *
         * <p>20=OK（成功）, 40=BAD_REQUEST（请求有问题）, 50=BAD_RESPONSE（响应有问题）
         * 类比 HTTP 的 200/400/500 状态码。
         * 当前框架中，成功时不需要显式设置，主要在出错时使用。
         */
        private int status;

        /**
         * 请求 ID —— 唯一的"快递单号"
         *
         * <p>使用雪花算法（Snowflake）生成的全局唯一 ID。
         * 作用：在异步通信中，一个连接上可能同时有多个请求在飞行中（in-flight），
         * 消费者收到响应时需要通过 requestId 来匹配"这个响应对应哪个请求"。
         *
         * <p>当前框架使用 CompletableFuture 同步等待，所以 requestId 主要用于日志追踪。
         * 未来升级为异步通信时，这个字段将至关重要。
         */
        private long requestId;

        /**
         * 消息体长度（字节数） —— "信纸有多厚"
         *
         * <p>解码器读完 17 字节头部后，根据 bodyLength 精确读取对应长度的消息体。
         * 这是解决 TCP 粘包/拆包的关键——TCP 是流式协议，不保证消息边界，
         * 必须用长度字段来切分出完整的消息。
         *
         * <p>粘包示例：
         * <pre>
         *   发送端：[消息A][消息B][消息C]
         *   接收端可能收到：[消息A的前半][消息A后半+消息B前半][消息B后半+消息C]
         *   没有长度字段，根本不知道在哪里切分！
         * </pre>
         */
        private int bodyLength;
    }
}
