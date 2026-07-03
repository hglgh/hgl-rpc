package com.hgl.hglrpc.serializer;

/**
 * 序列化器接口 —— "翻译官的工作规范"（easy 模块简化版）
 *
 * <p>定义了序列化和反序列化两个核心方法。
 * Java 对象不能直接在网络上传输，必须先"翻译"成字节数组（序列化），
 * 对方收到后再"翻译"回对象（反序列化）。
 *
 * <pre>
 *   消费端:  RpcRequest 对象 ──序列化──→ 字节数组 ──网络传输──→
 *   服务端:  ──→ 字节数组 ──反序列化──→ RpcRequest 对象
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/8/29 15:52
 */
public interface Serializer {

    /**
     * 序列化 —— "把对象翻译成字节数组"
     *
     * @param object 要序列化的对象
     * @param <T>    对象类型
     * @return 序列化后的字节数组
     */
    <T> byte[] serialize(T object);

    /**
     * 反序列化 —— "把字节数组翻译回对象"
     *
     * @param bytes 字节数组
     * @param type  目标类型
     * @param <T>   对象类型
     * @return 反序列化后的对象
     */
    <T> T deserialize(byte[] bytes, Class<T> type);
}
