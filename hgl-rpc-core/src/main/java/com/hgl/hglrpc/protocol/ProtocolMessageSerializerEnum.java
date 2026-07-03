package com.hgl.hglrpc.protocol;

import cn.hutool.core.util.ObjectUtil;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 序列化器枚举 —— "翻译官的花名册"
 *
 * <p>RPC 框架支持多种序列化方式，每种方式就像一位"翻译官"，
 * 负责把 Java 对象翻译成字节流（序列化），以及把字节流翻译回 Java 对象（反序列化）。
 *
 * <p>这个枚举维护了所有可用"翻译官"的编号和名称：
 * <pre>
 *   编号(key)  名称(value)   特点
 *   ─────────  ───────────   ──────────────────────────────────────
 *   0          jdk           Java 原生序列化，兼容性好但性能差、体积大
 *   1          json          JSON 序列化，可读性好但性能一般
 *   2          kryo          Kryo 序列化，速度快、体积小，但需要注册类
 *   3          hessian       Hessian 序列化，跨语言支持好，Dubbo 默认使用
 * </pre>
 *
 * <p>在协议报文中，serializer 字段存储的是 key（数字编号），因为数字只需 1 字节，
 * 而字符串需要 N 字节。解码器通过 key 查找这个枚举，得到对应的名称，
 * 再通过名称从 SPI 中加载对应的序列化器实例。
 *
 * @Author HGL
 * @Create: 2025/9/3 15:01
 */
@Getter
public enum ProtocolMessageSerializerEnum {
    /** JDK 原生序列化 —— Java 内置，开箱即用，但速度慢、体积大 */
    JDK(0, "jdk"),
    /** JSON 序列化 —— 人类可读，便于调试，但需要额外的类型信息处理 */
    JSON(1, "json"),
    /** Kryo 序列化 —— 高性能，体积小，推荐在内部系统间使用 */
    KRYO(2, "kryo"),
    /** Hessian 序列化 —— 跨语言，Dubbo 默认，适合异构系统间通信 */
    HESSIAN(3, "hessian");

    /** 序列化器的数字编号（写入协议报文） */
    private final int key;

    /** 序列化器的名称（用于 SPI 加载） */
    private final String value;

    ProtocolMessageSerializerEnum(int key, String value) {
        this.key = key;
        this.value = value;
    }

    /**
     * 获取所有序列化器的名称列表
     *
     * @return 所有 value 的列表
     */
    public static List<String> getValues() {
        return Arrays.stream(values()).map(item -> item.value).collect(Collectors.toList());
    }

    /**
     * 根据数字编号查找枚举 —— "按工号找翻译官"
     *
     * <p>解码器读到协议头部的 serializer 字段（数字）后，
     * 调用这个方法找到对应的枚举，进而获取序列化器的名称来加载实例。
     *
     * @param key 序列化器编号（协议报文中的数字）
     * @return 对应的枚举，找不到返回 null
     */
    public static ProtocolMessageSerializerEnum getEnumByKey(int key) {
        for (ProtocolMessageSerializerEnum anEnum : ProtocolMessageSerializerEnum.values()) {
            if (anEnum.key == key) {
                return anEnum;
            }
        }
        return null;
    }


    /**
     * 根据名称查找枚举 —— "按名字找翻译官"
     *
     * <p>配置文件中通常写的是名称（如 "kryo"），通过这个方法转为枚举，
     * 再获取 key 写入协议头部。
     *
     * @param value 序列化器名称（如 "jdk", "json", "kryo", "hessian"）
     * @return 对应的枚举，找不到返回 null
     */
    public static ProtocolMessageSerializerEnum getEnumByValue(String value) {
        if (ObjectUtil.isEmpty(value)) {
            return null;
        }
        for (ProtocolMessageSerializerEnum anEnum : ProtocolMessageSerializerEnum.values()) {
            if (anEnum.value.equals(value)) {
                return anEnum;
            }
        }
        return null;
    }
}
