package com.hgl.hglrpc.serializer;

import java.io.IOException;

/**
 * @ClassName: Serializer
 * @Package: com.hgl.hglrpc.serializer
 * @Description:
 * @Author HGL
 * @Create: 2025/8/29 15:52
 */
public interface Serializer {
    /**
     * 序列化
     *
     * @param object 对象
     * @param <T>    泛型
     * @return byte[]
     * @throws IOException 抛出IO异常
     */
    <T> byte[] serialize(T object) throws IOException;

    /**
     * 反序列化
     *
     * @param bytes 字节数组
     * @param type  类型
     * @param <T>   泛型
     * @return 对象
     * @throws IOException 抛出IO异常
     */
    <T> T deserialize(byte[] bytes, Class<T> type) throws IOException;
}
