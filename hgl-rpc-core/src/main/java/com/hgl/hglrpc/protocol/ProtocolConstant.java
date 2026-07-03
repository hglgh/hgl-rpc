package com.hgl.hglrpc.protocol;

/**
 * 协议常量 —— "交通规则的基石"
 *
 * <p>自定义二进制协议就像一套交通规则，规定了数据在网络中"行驶"的格式。
 * ProtocolConstant 定义了这套规则中最基本的三个常量，
 * 所有编解码器（Encoder/Decoder）都必须严格遵守。
 *
 * <p>协议报文结构（共 17 字节头 + 变长体）：
 * <pre>
 *   ┌─────────┬─────────┬────────────┬────────┬────────┬────────────┬────────────┬───────────┐
 *   │ magic   │ version │ serializer │  type  │ status │ requestId  │ bodyLength │   body    │
 *   │ 1 byte  │ 1 byte  │  1 byte    │ 1 byte │ 1 byte │  8 bytes   │  4 bytes   │ N bytes   │
 *   └─────────┴─────────┴────────────┴────────┴────────┴────────────┴────────────┴───────────┘
 *   |&lt;----------- 17 字节消息头 ----------&gt;|&lt;------------ 变长消息体 ------------&gt;|
 * </pre>
 *
 * <p>为什么需要魔数？
 * 就像人民币上的水印，魔数用于快速鉴别"这包数据是不是我认识的协议"。
 * 如果收到的数据第一个字节不是魔数，说明要么是垃圾数据，要么是别的协议，
 * 可以直接丢弃，避免浪费 CPU 去解析无意义的内容。
 *
 * @Author HGL
 * @Create: 2025/9/3 14:56
 */
public interface ProtocolConstant {

    /**
     * 消息头长度 = 17 字节
     *
     * <p>组成：magic(1) + version(1) + serializer(1) + type(1) + status(1) + requestId(8) + bodyLength(4)
     * 解码器在读取数据时，先读 17 字节解析出头部，再根据 bodyLength 读取剩余的消息体。
     * 这就是解决 TCP 粘包/拆包问题的核心思路——"定长头 + 变长体"。
     */
    int MESSAGE_HEADER_LENGTH = 17;

    /**
     * 协议魔数 —— 协议的"指纹"
     *
     * <p>值为 0x1（十六进制的 1）。
     * 实际生产环境中，建议使用更独特的值（如 0xCA），避免与其他协议巧合冲突。
     * 类比：Dubbo 的魔数是 0xdabb，HTTP 的魔数是 "GET"/"POST" 等文本前缀。
     */
    int PROTOCOL_MAGIC = 0x1;

    /**
     * 协议版本号 —— 协议的"版本标记"
     *
     * <p>值为 0x1，表示当前是第一版协议。
     * 未来如果协议格式有变（比如增加新的头部字段），
     * 可以通过版本号来区分新旧协议，实现向后兼容。
     * 类比：HTTP/1.0 vs HTTP/1.1 vs HTTP/2.0。
     */
    int PROTOCOL_VERSION = 0x1;
}
