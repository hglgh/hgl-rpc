package com.hgl.hglrpc.serializer;

/**
 * 序列化器接口 —— "翻译官的工作规范"
 *
 * <p>RPC 框架需要把 Java 对象通过网络发送给远端，
 * 但网络只认识字节流（byte[]），不认识 Java 对象。
 * 序列化器就是"翻译官"——负责在 Java 对象和字节流之间互相翻译。
 *
 * <p>工作流程：
 * <pre>
 *   消费者端：                                       提供者端：
 *   Java对象 ──serialize()──→ byte[] ──网络──→ byte[] ──deserialize()──→ Java对象
 *              （序列化：对象→字节）                        （反序列化：字节→对象）
 * </pre>
 *
 * <p>本框架内置了四种"翻译官"：
 * <pre>
 *   JdkSerializer      —— Java 原生翻译官，兼容性最好，但"翻译速度慢"、"译文冗长"
 *   JsonSerializer      —— JSON 翻译官，"译文"人类可读，便于调试
 *   KryoSerializer      —— Kryo 翻译官，速度极快、译文精简，推荐内部使用
 *   HessianSerializer   —— Hessian 翻译官，跨语言能力强，Dubbo 同款
 * </pre>
 *
 * <p>注意：接口方法不抛出 checked exception（如 IOException），
 * 而是抛出 unchecked 的 {@link SerializeException}。
 * 这样调用方不必到处 try-catch，只在真正需要处理的地方捕获即可。
 *
 * @Author HGL
 * @Create: 2025/8/29 15:52
 */
public interface Serializer {
    /**
     * 序列化 —— "把 Java 对象翻译成字节流"
     *
     * @param object 要序列化的 Java 对象
     * @param <T>    对象类型
     * @return 序列化后的字节数组
     * @throws SerializeException 序列化失败时抛出
     */
    <T> byte[] serialize(T object);

    /**
     * 反序列化 —— "把字节流翻译回 Java 对象"
     *
     * @param bytes 字节数组
     * @param type  目标类型（翻译官需要知道"翻译成什么语言"）
     * @param <T>   目标类型
     * @return 反序列化后的 Java 对象
     * @throws SerializeException 反序列化失败时抛出
     */
    <T> T deserialize(byte[] bytes, Class<T> type);
}
