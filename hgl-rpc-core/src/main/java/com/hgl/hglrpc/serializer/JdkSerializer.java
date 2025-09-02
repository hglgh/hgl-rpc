package com.hgl.hglrpc.serializer;

import java.io.*;

/**
 * @ClassName: JdkSerializer
 * @Package: com.hgl.hglrpc.serializer
 * @Description: JDK 序列化器
 * @Author HGL
 * @Create: 2025/8/29 15:55
 */
public class JdkSerializer implements Serializer {
    @Override
    public <T> byte[] serialize(T object) throws IOException {
        try (
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                // 装饰器模式，创建一个字节数组输出流，然后创建一个ObjectOutputStream对象，将字节数组输出流转换为ObjectOutputStream对象
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream)
        ) {
            objectOutputStream.writeObject(object);
            return outputStream.toByteArray();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> type) throws IOException {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
             ObjectInputStream objectInputStream = new ObjectInputStream(inputStream)) {
            return (T) objectInputStream.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
    }
}
