package com.hgl.hglrpc.protocol;

import lombok.Getter;

/**
 * @ClassName: ProtocolMessageTypeEnum
 * @Package: com.hgl.hglrpc.protocol
 * @Description: 协议消息的类型枚举
 * @Author HGL
 * @Create: 2025/9/3 15:00
 */
@Getter
public enum ProtocolMessageTypeEnum {
    REQUEST(0),
    RESPONSE(1),
    HEART_BEAT(2),
    OTHERS(3);

    private final int key;

    ProtocolMessageTypeEnum(int key) {
        this.key = key;
    }

    /**
     * 根据 key 获取枚举
     *
     * @param key key
     * @return 枚举
     */
    public static ProtocolMessageTypeEnum getEnumByKey(int key) {
        for (ProtocolMessageTypeEnum anEnum : ProtocolMessageTypeEnum.values()) {
            if (anEnum.key == key) {
                return anEnum;
            }
        }
        return null;
    }
}
