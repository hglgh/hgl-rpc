package com.hgl.hglrpc.serializer;

import java.io.*;

/**
 * JDK 序列化器 —— "Java 原生翻译官"
 *
 * <p>使用 Java 内置的 ObjectOutputStream / ObjectInputStream 进行序列化/反序列化。
 * 这是最"传统"的翻译官——不需要额外依赖，开箱即用。
 *
 * <p>原理：
 * <pre>
 *   序列化：Java对象 → ObjectOutputStream.writeObject() → 字节流
 *   反序列化：字节流 → ObjectInputStream.readObject() → Java对象
 * </pre>
 *
 * <p>优缺点：
 * <pre>
 *   ✅ 优点：无需第三方依赖，任何实现了 Serializable 接口的类都能序列化
 *   ❌ 缺点：1. 性能差（大量使用反射）
 *            2. 序列化后体积大（包含完整的类元信息）
 *            3. 存在安全漏洞（反序列化攻击）
 * </pre>
 *
 * <p>注意：被序列化的对象必须实现 {@link java.io.Serializable} 接口，
 * 否则会抛出 NotSerializableException（被包装为 SerializeException）。
 *
 * @Author HGL
 * @Create: 2025/8/29 15:55
 */
public class JdkSerializer implements Serializer {
    @Override
    public <T> byte[] serialize(T object) {
        try (
                // ByteArrayOutputStream：在内存中开辟一块缓冲区来接收字节
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                // ObjectOutputStream：把 Java 对象"灌"进字节流
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)
        ) {
            objectOutputStream.writeObject(object);
            return outputStream.toByteArray();
        } catch (IOException e) {
            // 包装为非受检异常，调用方不必强制处理
            throw new SerializeException("JDK 序列化失败", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> type) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
             ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            // readObject() 返回 Object，需要强制转型
            return (T) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new SerializeException("JDK 反序列化失败", e);
        }
    }
}
