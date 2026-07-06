package com.hgl.hglrpc.server.tcp.codec;

import com.hgl.hglrpc.model.RpcRequest;
import com.hgl.hglrpc.model.RpcResponse;
import com.hgl.hglrpc.protocol.ProtocolConstant;
import com.hgl.hglrpc.protocol.ProtocolMessage;
import com.hgl.hglrpc.protocol.ProtocolMessageSerializerEnum;
import com.hgl.hglrpc.protocol.ProtocolMessageTypeEnum;
import com.hgl.hglrpc.serializer.Serializer;
import com.hgl.hglrpc.serializer.SerializerFactory;
import com.hgl.hglrpc.server.tcp.TcpBufferHandlerWrapper;
import io.vertx.core.buffer.Buffer;

/**
 * 协议解码器 —— "拆开快递箱取出信纸"
 *
 * <p>ProtocolMessageDecoder 负责将网络上收到的二进制字节流（{@link Buffer}）
 * 解码还原为 Java 对象 {@link ProtocolMessage}。
 *
 * <p>解码是编码的逆过程，就像"拆快递"：
 * <pre>
 *   ┌──────────────────────────────────────────────────────────────────────┐
 *   │                    解码流程（拆快递）                                    │
 *   │                                                                      │
 *   │  二进制 Buffer（从网络收到的原始数据）                                     │
 *   │       │                                                              │
 *   │       ▼                                                              │
 *   │  ┌────────────────────────────────────────────────────────────────┐  │
 *   │  │  第1步：验真（校验魔数）                                           │  │
 *   │  │                                                                │  │
 *   │  │  读取 byte 0，检查是否等于 PROTOCOL_MAGIC                          │  │
 *   │  │  如果不匹配 → 抛异常"消息 magic 非法"                               │  │
 *   │  │  类比：收到"银行来信"，先看有没有银行印章                              │  │
 *   │  └────────────────────────────────────────────────────────────────┘  │
 *   │       │                                                              │
 *   │       ▼                                                              │
 *   │  ┌────────────────────────────────────────────────────────────────┐  │
 *   │  │  第2步：读快递单（解析 Header 各字段）                               │  │
 *   │  │                                                               │  │
 *   │  │  byte  0:   magic       → header.magic                        │  │
 *   │  │  byte  1:   version     → header.version                      │  │
 *   │  │  byte  2:   serializer  → header.serializer                   │  │
 *   │  │  byte  3:   type        → header.type                         │  │
 *   │  │  byte  4:   status      → header.status                       │  │
 *   │  │  byte  5-12: requestId  → header.requestId (long)             │  │
 *   │  │  byte 13-16: bodyLength → header.bodyLength (int)             │  │
 *   │  │                                                               │  │
 *   │  │  注意：& 0xFF 将 byte 转为无符号 int                              │  │
 *   │  │  Java 的 byte 是有符号的（-128~127），直接转 int 会出现符号问题       │  │
 *   │  │  例如：byte 0xFF = -1，但 int 应该是 255                          │  │
 *   │  └────────────────────────────────────────────────────────────────┘  │
 *   │       │                                                              │
 *   │       ▼                                                              │
 *   │  ┌────────────────────────────────────────────────────────────────┐  │
 *   │  │  第3步：精确取件（按 bodyLength 读取 body）                         │  │
 *   │  │                                                                │  │
 *   │  │  bodyBytes = buffer.getBytes(17, 17 + bodyLength)              │  │
 *   │  │  只读指定长度，不读多余数据 —— 这就是解决粘包的关键！                    │  │
 *   │  │  即使 Buffer 中后面还有其他消息的数据，也不会误读                      │  │
 *   │  └────────────────────────────────────────────────────────────────┘  │
 *   │       │                                                              │
 *   │       ▼                                                              │
 *   │  ┌────────────────────────────────────────────────────────────────┐  │
 *   │  │  第4步：拆信封（反序列化 Body）                                     │  │
 *   │  │                                                                │  │
 *   │  │  根据 header.serializer 选择反序列化器：                           │  │
 *   │  │    JDK(0) / JSON(1) / Kryo(2) / Hessian(3)                     │  │
 *   │  │  根据 header.type 确定反序列化目标类型：                            │  │
 *   │  │    REQUEST  → RpcRequest                                       │  │
 *   │  │    RESPONSE → RpcResponse                                      │  │
 *   │  │    HEART_BEAT / OTHERS → 暂不支持                                │  │
 *   │  └────────────────────────────────────────────────────────────────┘  │
 *   │       │                                                              │
 *   │       ▼                                                              │
 *   │  ProtocolMessage 对象（完整的信封 + 信纸）                                │
 *   └──────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/9/3 16:41
 * @see ProtocolMessageEncoder 协议编码器（封装快递）
 * @see ProtocolMessage 协议消息结构（信封 + 信纸）
 * @see TcpBufferHandlerWrapper 粘包拆包处理器，保证传入此解码器的 Buffer 是一条完整消息
 */
public class ProtocolMessageDecoder {

    /**
     * 解码协议消息 —— "拆开快递箱"
     *
     * <p>从 Buffer 中按照自定义二进制协议格式读取头部和消息体，
     * 反序列化后还原为 {@link ProtocolMessage} 对象。
     *
     * <p>前置条件：传入的 buffer 必须是一条<b>完整的消息</b>（由 {@link TcpBufferHandlerWrapper} 保证）。
     *
     * @param buffer 包含一条完整协议消息的缓冲区
     * @return 还原后的 ProtocolMessage 对象
     * @throws RuntimeException 魔数校验失败、序列化方式不支持、消息类型不支持等情况
     */
    public static ProtocolMessage<?> decode(Buffer buffer) {
        // ========== 第1步 & 第2步：验真 + 读快递单（解析 Header） ==========
        ProtocolMessage.Header header = new ProtocolMessage.Header();

        // 读取魔数并校验 —— "先看有没有防伪标识"
        // & 0xFF：将有符号 byte 转为无符号 int
        // 例如：byte 0xFF = -1，& 0xFF 后变成 int 255
        int magic = buffer.getByte(0) & 0xFF;
        if (magic != ProtocolConstant.PROTOCOL_MAGIC) {
            throw new RuntimeException("消息 magic 非法");
        }
        header.setMagic(magic);

        // 依次读取头部各字段 —— "逐栏填写快递单信息"
        header.setVersion(buffer.getByte(1) & 0xFF);      // byte  1: 版本号
        header.setSerializer(buffer.getByte(2) & 0xFF);    // byte  2: 序列化器
        header.setType(buffer.getByte(3) & 0xFF);          // byte  3: 消息类型
        header.setStatus(buffer.getByte(4) & 0xFF);        // byte  4: 状态码
        header.setRequestId(buffer.getLong(5));             // byte  5: 请求ID（8字节 long）
        header.setBodyLength(buffer.getInt(13));            // byte 13: body 长度（4字节 int）

        // ========== 第3步：精确取件（按 bodyLength 读取 body） ==========
        // 从 offset 17 开始，精确读取 bodyLength 个字节
        // 这解决了粘包问题——即使 buffer 后面还有其他消息的数据，也不会误读
        byte[] bodyBytes = buffer.getBytes(17, 17 + header.getBodyLength());

        // ========== 第4步：拆信封（反序列化 Body） ==========
        // 获取序列化器 —— "这封信是用什么语言写的？"
        ProtocolMessageSerializerEnum serializerEnum =
                ProtocolMessageSerializerEnum.getEnumByKey(header.getSerializer());
        if (serializerEnum == null) {
            throw new RuntimeException("序列化消息的类型不存在");
        }
        Serializer serializer = SerializerFactory.getInstance(serializerEnum.getValue());

        // 根据消息类型选择反序列化目标类 —— "这是请求信还是回信？"
        ProtocolMessageTypeEnum messageTypeEnum =
                ProtocolMessageTypeEnum.getEnumByKey(header.getType());
        if (messageTypeEnum == null) {
            throw new RuntimeException("序列化消息的类型不存在");
        }

        switch (messageTypeEnum) {
            case REQUEST:
                // 请求消息 → 反序列化为 RpcRequest
                RpcRequest request = serializer.deserialize(bodyBytes, RpcRequest.class);
                return new ProtocolMessage<>(header, request);
            case RESPONSE:
                // 响应消息 → 反序列化为 RpcResponse
                RpcResponse response = serializer.deserialize(bodyBytes, RpcResponse.class);
                return new ProtocolMessage<>(header, response);
            case HEART_BEAT:
            case OTHERS:
            default:
                // 心跳和其他类型暂不支持
                throw new RuntimeException("暂不支持该消息类型");
        }
    }
}
