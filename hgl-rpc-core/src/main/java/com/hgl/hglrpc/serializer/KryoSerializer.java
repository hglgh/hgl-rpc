package com.hgl.hglrpc.serializer;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/**
 * Kryo 序列化器 —— "极速翻译官"
 *
 * <p>Kryo 是一个高性能的 Java 序列化框架，以"快"和"小"著称。
 * 它不走 Java 原生序列化的反射路线，而是直接操作字节，性能可提升 10~100 倍。
 *
 * <p>原理：
 * <pre>
 *   序列化：Java对象 → Kryo.writeObject() → 极紧凑的二进制字节流
 *   反序列化：二进制字节流 → Kryo.readObject() → Java对象
 * </pre>
 *
 * <p>优缺点：
 * <pre>
 *   ✅ 优点：1. 速度极快（通常是 JDK 序列化的 10 倍以上）
 *            2. 体积最小（比 JDK 小 5~10 倍，比 Hessian 也小）
 *            3. API 简洁易用
 *   ❌ 缺点：1. 不支持跨语言（仅限 Java）
 *            2. Kryo 实例非线程安全，需要 ThreadLocal 保护
 *            3. 反序列化时需要知道目标类型
 * </pre>
 *
 * <p>线程安全处理：
 * Kryo 实例不是线程安全的（内部维护了写入状态），所以不能全局共享一个实例。
 * 这里用 ThreadLocal 为每个线程创建独立的 Kryo 实例，
 * 既保证线程安全，又避免频繁创建/销毁的开销。
 *
 * <p>setRegistrationRequired(false)：
 * Kryo 默认要求提前注册所有要序列化的类（性能更好），但使用不方便。
 * 设置为 false 后，Kryo 会自动处理未注册的类，适合通用 RPC 框架。
 *
 * @Author HGL
 * @Create: 2025/9/1 16:46
 */
public class KryoSerializer implements Serializer {

    /**
     * Kryo 线程不安全，使用 ThreadLocal 保证每个线程有独立的 Kryo 实例
     *
     * <p>ThreadLocal 就像给每个员工发了一套专属文具——
     * 不用抢、不用锁、用完也不用还（线程销毁时自动回收）。
     */
    private static final ThreadLocal<Kryo> KRYO_THREAD_LOCAL = ThreadLocal.withInitial(() -> {
        Kryo kryo = new Kryo();
        // 不强制注册类，允许序列化任意对象（牺牲少量性能换取便利性）
        kryo.setRegistrationRequired(false);
        return kryo;
    });

    @Override
    public <T> byte[] serialize(T object) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        Output output = new Output(byteArrayOutputStream);
        // 从 ThreadLocal 获取当前线程专属的 Kryo 实例
        KRYO_THREAD_LOCAL.get().writeObject(output, object);
        output.close();
        return byteArrayOutputStream.toByteArray();
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> classType) {
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bytes);
        Input input = new Input(byteArrayInputStream);
        T result = KRYO_THREAD_LOCAL.get().readObject(input, classType);
        input.close();
        return result;
    }
}
