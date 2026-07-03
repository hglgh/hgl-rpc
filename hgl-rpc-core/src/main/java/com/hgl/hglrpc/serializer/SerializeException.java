package com.hgl.hglrpc.serializer;

/**
 * 序列化异常 —— "翻译失败的通知单"
 *
 * <p>当序列化或反序列化失败时，抛出此异常。
 * 它是 RuntimeException（非受检异常），调用方不必强制 try-catch，
 * 只在真正需要处理错误的层级捕获即可。
 *
 * <p>为什么不继续使用 IOException？
 * 因为 Kryo、Hessian 等序列化器本身抛出的是运行时异常，
 * 如果 Serializer 接口声明 throws IOException，
 * Kryo/Hessian 的实现被迫把运行时异常包装成 IOException，
 * 调用方又被迫到处 try-catch IOException —— 层层包装，徒增复杂度。
 *
 * @Author HGL
 * @Create: 2025/9/8
 */
public class SerializeException extends RuntimeException {
    public SerializeException(String message) {
        super(message);
    }

    public SerializeException(String message, Throwable cause) {
        super(message, cause);
    }
}
