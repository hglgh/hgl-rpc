package com.hgl.hglrpc.server.tcp.codec;

import com.hgl.hglrpc.protocol.ProtocolMessage;
import com.hgl.hglrpc.protocol.ProtocolMessageSerializerEnum;
import com.hgl.hglrpc.serializer.Serializer;
import com.hgl.hglrpc.serializer.SerializerFactory;
import io.vertx.core.buffer.Buffer;

/**
 * 协议编码器 —— "把信封装进快递箱"
 *
 * <p>ProtocolMessageEncoder 负责将 Java 对象 {@link ProtocolMessage} 编码为
 * 可以在网络上传输的二进制字节流（Vert.x {@link Buffer}）。
 *
 * <p>编码过程就像"封装快递"：
 * <pre>
 *   ┌──────────────────────────────────────────────────────────────────────┐
 *   │                    编码流程（封装快递）                               │
 *   │                                                                      │
 *   │  Java 对象（ProtocolMessage）                                        │
 *   │       │                                                              │
 *   │       ▼                                                              │
 *   │  ┌────────────────────────────────────────────────────────────────┐  │
 *   │  │  第1步：填写快递单（Header 各字段）                            │  │
 *   │  │                                                                │  │
 *   │  │  byte  0:   magic      ← 协议防伪标识                         │  │
 *   │  │  byte  1:   version    ← 协议版本号                           │  │
 *   │  │  byte  2:   serializer ← 序列化方式标识                       │  │
 *   │  │  byte  3:   type       ← 消息类型（请求/响应）                 │  │
 *   │  │  byte  4:   status     ← 状态码                               │  │
 *   │  │  byte  5-12: requestId ← 请求ID（8字节 long）                 │  │
 *   │  └────────────────────────────────────────────────────────────────┘  │
 *   │       │                                                              │
 *   │       ▼                                                              │
 *   │  ┌────────────────────────────────────────────────────────────────┐  │
 *   │  │  第2步：装箱（序列化 Body）                                    │  │
 *   │  │                                                                │  │
 *   │  │  根据 header.serializer 选择序列化器：                         │  │
 *   │  │    JDK(0) / JSON(1) / Kryo(2) / Hessian(3)                    │  │
 *   │  │  将 body（RpcRequest/RpcResponse）序列化为 byte[]              │  │
 *   │  └────────────────────────────────────────────────────────────────┘  │
 *   │       │                                                              │
 *   │       ▼                                                              │
 *   │  ┌────────────────────────────────────────────────────────────────┐  │
 *   │  │  第3步：贴标签（写 bodyLength + body）                        │  │
 *   │  │                                                                │  │
 *   │  │  byte 13-16: bodyLength  ← body 字节数（4字节 int）           │  │
 *   │  │  byte 17+:   bodyBytes   ← 序列化后的消息体                   │  │
 *   │  └────────────────────────────────────────────────────────────────┘  │
 *   │       │                                                              │
 *   │       ▼                                                              │
 *   │  二进制 Buffer（可以发送到网络了！）                                 │
 *   │                                                                      │
 *   │  最终格式：                                                          │
 *   │  ┌─────┬─────┬─────┬─────┬──────┬──────────┬──────────┬──────────┐  │
 *   │  │magic│ver  │ser  │type │status│requestId │bodyLength│   body   │  │
 *   │  │1B   │1B   │1B   │1B   │1B    │8B        │4B        │ N B     │  │
 *   │  └─────┴─────┴─────┴─────┴──────┴──────────┴──────────┴──────────┘  │
 *   │  |&lt;-------------- 17 字节消息头 ------------&gt;|&lt;--- 变长消息体 ---&gt;|  │
 *   └──────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/9/3 16:35
 * @see ProtocolMessageDecoder 协议解码器（拆快递）
 * @see ProtocolMessage 协议消息结构（信封 + 信纸）
 */
public class ProtocolMessageEncoder {

    /**
     * 编码协议消息 —— "封装快递并发货"
     *
     * <p>将 {@link ProtocolMessage} 对象编码为 Vert.x {@link Buffer}，
     * 按照自定义二进制协议格式依次写入头部字段和序列化后的消息体。
     *
     * @param protocolMessage 待编码的协议消息（信封 + 信纸）
     * @return 编码后的 Buffer（封装好的快递箱），可直接写入 NetSocket
     */
    public static Buffer encode(ProtocolMessage<?> protocolMessage) {
        // 空值校验 —— "没有包裹要寄？那就算了"
        if (protocolMessage == null || protocolMessage.getHeader() == null) {
            return Buffer.buffer();
        }

        ProtocolMessage.Header header = protocolMessage.getHeader();

        // ========== 第1步：填写快递单（依次写入头部各字段） ==========
        Buffer buffer = Buffer.buffer();
        buffer.appendByte((byte) header.getMagic());       // offset  0: 魔数（1字节）
        buffer.appendByte((byte) header.getVersion());     // offset  1: 版本号（1字节）
        buffer.appendByte((byte) header.getSerializer());  // offset  2: 序列化器（1字节）
        buffer.appendByte((byte) header.getType());        // offset  3: 消息类型（1字节）
        buffer.appendByte((byte) header.getStatus());      // offset  4: 状态码（1字节）
        buffer.appendLong(header.getRequestId());          // offset  5: 请求ID（8字节）
        // 此时写了 13 字节，接下来写 bodyLength（4字节）和 body

        // ========== 第2步：装箱（序列化消息体） ==========
        // 根据头部的 serializer 字段获取对应的序列化器
        ProtocolMessageSerializerEnum serializerEnum =
                ProtocolMessageSerializerEnum.getEnumByKey(header.getSerializer());
        if (serializerEnum == null) {
            throw new RuntimeException("序列化协议不存在");
        }
        Serializer serializer = SerializerFactory.getInstance(serializerEnum.getValue());
        // 将 body 对象序列化为 byte 数组 —— "把信纸折好放进信封"
        byte[] bodyBytes = serializer.serialize(protocolMessage.getBody());

        // ========== 第3步：贴标签 + 装箱（写 bodyLength 和 body） ==========
        buffer.appendInt(bodyBytes.length);    // offset 13: body 长度（4字节）
        buffer.appendBytes(bodyBytes);         // offset 17: body 内容（N字节）

        return buffer;
    }
}
