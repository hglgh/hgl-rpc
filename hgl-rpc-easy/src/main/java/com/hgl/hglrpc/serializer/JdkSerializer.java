package com.hgl.hglrpc.serializer;

import java.io.*;

/**
 * JDK 原生序列化器 —— "Java 自带的翻译官"（easy 模块简化版）
 *
 * <p>使用 Java 原生的 {@link ObjectOutputStream} / {@link ObjectInputStream}
 * 进行序列化和反序列化。优点是零依赖，缺点是序列化后体积大、速度慢。
 *
 * <p>要求被序列化的类必须实现 {@link java.io.Serializable} 接口。
 *
 * @Author HGL
 * @Create: 2025/8/29 15:55
 */
public class JdkSerializer implements Serializer {

    @Override
    public <T> byte[] serialize(T object) {
        try (
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)
        ) {
            // 把对象写入字节流
            objectOutputStream.writeObject(object);
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("序列化失败", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> type) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        try (ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            // 从字节流中读取对象
            return (T) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException("反序列化失败", e);
        }
    }
}
