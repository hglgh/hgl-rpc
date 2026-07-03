package com.hgl.hglrpc.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * RPC 请求对象 —— "远程调用的快递单"（easy 模块简化版）
 *
 * <p>当消费者需要调用远程服务时，把所有信息打包成这个"快递单"发送给服务提供者。
 * 提供者收到后，按照快递单上的信息找到对应的方法并执行。
 *
 * <pre>
 *   ┌─────────────────────────────────────────────────────────┐
 *   │                    RpcRequest（快递单）                    │
 *   ├─────────────────────────────────────────────────────────┤
 *   │ serviceName      → "UserService"    找哪家店？           │
 *   │ methodName       → "getUserById"    办什么业务？          │
 *   │ parameterTypes   → [Long.class]    需要什么材料？（类型）  │
 *   │ args             → [1L]            材料是什么？（值）      │
 *   └─────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/8/29 16:10
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RpcRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 服务名称 —— "去哪家店办业务" */
    private String serviceName;

    /** 方法名称 —— "办什么业务" */
    private String methodName;

    /** 参数类型列表 —— "需要什么材料（类型）"，用于反射定位方法 */
    private Class<?>[] parameterTypes;

    /** 参数列表 —— "材料的实际内容"，即方法的实际入参 */
    private Object[] args;
}
