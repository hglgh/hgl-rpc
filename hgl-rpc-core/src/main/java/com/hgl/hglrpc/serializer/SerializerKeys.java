package com.hgl.hglrpc.serializer;

/**
 * 序列化器键名常量 —— "翻译官的工号"
 *
 * <p>每种序列化器都有一个字符串名称（如 "jdk", "kryo"），
 * 这个名称用于：
 *   1. 配置文件中指定使用哪种序列化器（rpc.serializer=kryo）
 *   2. SPI 配置文件中映射名称到实现类
 *   3. 工厂类中根据名称获取序列化器实例
 *
 * <p>使用常量而非硬编码字符串，可以避免拼写错误导致的运行时问题。
 * 编译器会在拼写错误时直接报错，而不是运行时才发现 "kyro" 找不到。
 *
 * @Author HGL
 * @Create: 2025/9/1 16:51
 */
public interface SerializerKeys {
    /** JDK 原生序列化 —— 默认选项，兼容性最好 */
    String JDK = "jdk";
    /** JSON 序列化 —— 人类可读，便于调试 */
    String JSON = "json";
    /** Kryo 序列化 —— 高性能，推荐内部系统使用 */
    String KRYO = "kryo";
    /** Hessian 序列化 —— 跨语言，适合异构系统 */
    String HESSIAN = "hessian";
}
