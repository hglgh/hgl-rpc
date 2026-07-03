package com.hgl.hglrpc.protocol;

import lombok.Getter;

/**
 * 协议消息类型枚举 —— "邮件的种类标签"
 *
 * <p>在网络通信中，一条消息可能是"请帮我办事"（REQUEST），
 * 也可能是"事情办好了，结果给你"（RESPONSE），
 * 还可能是"我还活着，别断开连接"（HEART_BEAT）。
 *
 * <p>消息类型写在协议头部的 type 字段中（仅占 1 字节），
 * 服务端收到消息后，先看 type 再决定怎么处理：
 * <pre>
 *   type=0 (REQUEST)   → 解码消息体为 RpcRequest，反射调用目标方法
 *   type=1 (RESPONSE)  → 解码消息体为 RpcResponse，返回给调用者
 *   type=2 (HEART_BEAT)→ 心跳包，回复一个心跳确认即可
 *   type=3 (OTHERS)    → 预留，未来可扩展（如：服务发现通知、配置推送等）
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/9/3 15:00
 */
@Getter
public enum ProtocolMessageTypeEnum {
    /** 请求消息 —— "请你帮我调用某个方法" */
    REQUEST(0),
    /** 响应消息 —— "方法调用完了，这是结果" */
    RESPONSE(1),
    /** 心跳消息 —— "我还活着，连接别断" */
    HEART_BEAT(2),
    /** 其他类型 —— 预留扩展 */
    OTHERS(3);

    /** 消息类型的数字编号 */
    private final int key;

    ProtocolMessageTypeEnum(int key) {
        this.key = key;
    }

    /**
     * 根据编号查找枚举
     *
     * @param key 消息类型编号
     * @return 对应的枚举，找不到返回 null
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
