package com.hgl.hglrpc.protocol;

/**
 * @ClassName: ProtocolConstant
 * @Package: com.hgl.hglrpc.protocol
 * @Description: 协议常量
 * @Author HGL
 * @Create: 2025/9/3 14:56
 */
public interface ProtocolConstant {

    /**
     * 消息头长度
     */
    int MESSAGE_HEADER_LENGTH = 17;

    /**
     * 协议魔数
     */
    byte PROTOCOL_MAGIC = 0x1;

    /**
     * 协议版本号
     */
    byte PROTOCOL_VERSION = 0x1;
}
