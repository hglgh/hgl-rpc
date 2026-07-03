package com.hgl.hglrpc.serializer;

import com.caucho.hessian.io.Hessian2Input;
import com.caucho.hessian.io.Hessian2Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Hessian 序列化器 —— "跨语言翻译官"
 *
 * <p>Hessian 是一种跨语言的二进制序列化协议，由 Caucho 公司开发。
 * 它是 Apache Dubbo 框架的默认序列化方式，久经生产环境考验。
 *
 * <p>原理：
 * <pre>
 *   序列化：Java对象 → Hessian2Output.writeObject() → 紧凑二进制字节流
 *   反序列化：二进制字节流 → Hessian2Input.readObject() → Java对象
 * </pre>
 *
 * <p>优缺点：
 * <pre>
 *   ✅ 优点：1. 跨语言（Java/Python/C++/C# 等都有实现）
 *            2. 序列化体积比 JDK 小得多
 *            3. 性能比 JDK 好
 *            4. 不需要实现 Serializable 接口也能序列化
 *   ❌ 缺点：1. 需要引入 hessian 依赖
 *            2. 性能和体积不如 Kryo
 * </pre>
 *
 * <p>适用场景：异构系统间通信（比如 Java 服务调用 Python 服务）。
 *
 * @Author HGL
 * @Create: 2025/9/1 16:49
 */
public class HessianSerializer implements Serializer {
    @Override
    public <T> byte[] serialize(T object) {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Hessian2Output hessian2Output = new Hessian2Output(outputStream);
            hessian2Output.writeObject(object);
            hessian2Output.flush();
            return outputStream.toByteArray();
        } catch (IOException e) {
            throw new SerializeException("Hessian 序列化失败", e);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T deserialize(byte[] bytes, Class<T> type) {
        try {
            ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
            Hessian2Input hi = new Hessian2Input(bis);
            // readObject(type) 指定目标类型，Hessian 会尝试按该类型反序列化
            return (T) hi.readObject(type);
        } catch (IOException e) {
            throw new SerializeException("Hessian 反序列化失败", e);
        }
    }
}
