package com.hgl.hglrpc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * RPC 响应对象 —— "远程调用的回执单"（easy 模块简化版）
 *
 * <p>服务提供者执行完方法后，把结果打包成"回执单"返回给消费者。
 *
 * <pre>
 *   ┌───────────────────────────────────────────────────┐
 *   │               RpcResponse（回执单）                 │
 *   ├───────────────────────────────────────────────────┤
 *   │ data       → User{id=1, name="HGL"}  业务结果     │
 *   │ dataType   → User.class              结果类型     │
 *   │ message    → "ok"                    处理结果描述  │
 *   │ exception  → null（或 Exception）     异常信息     │
 *   └───────────────────────────────────────────────────┘
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

    /** 响应数据 —— "业务办理的结果" */
    private Object data;

    /** 响应数据类型 —— "结果的类型信息"，预留字段 */
    private Class<?> dataType;

    /** 响应信息 —— "处理结果描述"（如 "ok" 或错误消息） */
    private String message;

    /** 异常信息 —— "业务办理过程中出了什么问题" */
    private Exception exception;
}
