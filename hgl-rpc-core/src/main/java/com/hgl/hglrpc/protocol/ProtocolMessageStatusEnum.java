package com.hgl.hglrpc.protocol;

import lombok.Getter;

/**
 * 协议消息状态枚举 —— "快递的签收状态"
 *
 * <p>每条 RPC 消息都有一个"状态码"，告诉对方这趟通信结果如何。
 * 类比 HTTP 状态码：200 成功、400 请求有问题、500 服务器出错。
 *
 * <p>状态码写在协议头部的 status 字段中（占 1 字节）。
 * 消费者收到响应后，先检查 status：
 * <pre>
 *   status=20 (OK)          → 取出 data，正常返回
 *   status=40 (BAD_REQUEST) → 请求格式有问题，检查 RpcRequest 构造
 *   status=50 (BAD_RESPONSE)→ 服务端处理出错，检查 exception 字段
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/9/3 14:59
 */
@Getter
public enum ProtocolMessageStatusEnum {

    /** 成功 —— "快递已签收，一切正常" */
    OK("ok", 20),
    /** 请求错误 —— "快递单填错了，没法派送" */
    BAD_REQUEST("badRequest", 40),
    /** 响应错误 —— "包裹在运输中损坏了" */
    BAD_RESPONSE("badResponse", 50);

    /** 状态的文本描述（用于日志和调试） */
    private final String text;

    /** 状态的数字编号（写入协议报文） */
    private final int value;

    ProtocolMessageStatusEnum(String text, int value) {
        this.text = text;
        this.value = value;
    }

    /**
     * 根据数字编号查找枚举
     *
     * @param value 状态码数字
     * @return 对应的枚举，找不到返回 null
     */
    public static ProtocolMessageStatusEnum getEnumByValue(int value) {
        for (ProtocolMessageStatusEnum anEnum : ProtocolMessageStatusEnum.values()) {
            if (anEnum.value == value) {
                return anEnum;
            }
        }
        return null;
    }
}
