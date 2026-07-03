package com.hgl.hglrpc.spi;

import cn.hutool.core.io.resource.ResourceUtil;
import com.hgl.hglrpc.serializer.Serializer;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SPI 加载器 —— "插件管理器"
 *
 * <p>SPI（Service Provider Interface）是 Java 的一种"插件化"机制。
 * 类比：你买了一台电脑（框架），它有 USB 接口（SPI），
 * 你可以插入不同的 USB 设备（实现类）——U盘、鼠标、键盘——而不需要修改电脑本身。
 *
 * <p>本框架的 SPI 机制是 JDK SPI 的增强版，支持 key=value 的映射方式。
 * JDK 原生 SPI 只能通过类名加载（一个接口对应一个实现），
 * 而本框架支持"同一个接口，根据不同的 key 加载不同的实现"。
 *
 * <p>SPI 配置文件示例（META-INF/rpc/system/com.hgl.hglrpc.serializer.Serializer）：
 * <pre>
 *   jdk=com.hgl.hglrpc.serializer.JdkSerializer
 *   json=com.hgl.hglrpc.serializer.JsonSerializer
 *   kryo=com.hgl.hglrpc.serializer.KryoSerializer
 *   hessian=com.hgl.hglrpc.serializer.HessianSerializer
 * </pre>
 *
 * <p>加载流程：
 * <pre>
 *   1. getInstance(Serializer.class, "kryo") 被调用
 *   2. 检查 LOADER_MAP 是否已加载过 Serializer 类型
 *   3. 如果没有，自动调用 load() 扫描配置文件
 *   4. 配置文件中找到 kryo=com.hgl.hglrpc.serializer.KryoSerializer
 *   5. 反射创建 KryoSerializer 实例，缓存到 INSTANCE_CACHE
 *   6. 返回实例（后续调用直接从缓存取，不再创建）
 * </pre>
 *
 * <p>两个扫描目录：
 * <pre>
 *   META-INF/rpc/system/  —— 框架内置的实现（如各种序列化器）
 *   META-INF/rpc/custom/  —— 用户自定义的实现（优先级更高，可以覆盖系统默认）
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/9/1 17:02
 */
@Slf4j
public class SpiLoader {

    /**
     * 已加载的类型映射表 —— "插件目录"
     *
     * <p>结构：接口全限定名 → (key → 实现类Class)
     * 例如："com.hgl.hglrpc.serializer.Serializer" → {"jdk" → JdkSerializer.class, "kryo" → KryoSerializer.class}
     */
    private static final Map<String, Map<String, Class<?>>> LOADER_MAP = new ConcurrentHashMap<>();

    /**
     * 实例缓存 —— "已创建的插件单例"
     *
     * <p>结构：实现类全限定名 → 实例对象
     * 确保每个实现类只创建一次实例（单例模式），避免重复创建浪费资源。
     */
    private static final Map<String, Object> INSTANCE_CACHE = new ConcurrentHashMap<>();

    /** 框架内置的 SPI 配置目录 */
    private static final String RPC_SYSTEM_SPI_DIR = "META-INF/rpc/system/";

    /** 用户自定义的 SPI 配置目录（优先级更高） */
    private static final String RPC_CUSTOM_SPI_DIR = "META-INF/rpc/custom/";

    /** 扫描目录列表（先扫系统目录，再扫自定义目录，后者可覆盖前者） */
    private static final String[] SCAN_DIRS = new String[]{RPC_SYSTEM_SPI_DIR, RPC_CUSTOM_SPI_DIR};

    /**
     * 需要动态加载的类型列表
     *
     * <p>当前只有 Serializer 使用了 SPI 机制。
     * 未来如果有更多可插拔的组件（如负载均衡器、注册中心等），
     * 可以添加到这个列表中。
     */
    private static final List<Class<?>> LOAD_CLASS_LIST = Collections.singletonList(Serializer.class);

    /**
     * 加载所有 SPI 类型 —— "启动时预热所有插件"
     */
    public static void loadAll() {
        log.info("加载所有 SPI");
        for (Class<?> aClass : LOAD_CLASS_LIST) {
            load(aClass);
        }
    }

    /**
     * 获取某个接口的实例 —— "按需取插件"
     *
     * <p>这是最常用的方法。调用时如果该类型尚未加载，会自动加载（懒加载）。
     * 获取到的实例是单例的，线程安全。
     *
     * @param tClass 接口类型（如 Serializer.class）
     * @param key    键名（如 "kryo"）
     * @param <T>    泛型
     * @return 实现类的单例实例
     * @throws RuntimeException 如果找不到对应的实现类
     */
    @SuppressWarnings("unchecked")
    public static <T> T getInstance(Class<?> tClass, String key) {
        String tClassName = tClass.getName();
        // 懒加载：如果该类型尚未加载，先执行 load
        if (!LOADER_MAP.containsKey(tClassName)) {
            load(tClass);
        }
        Map<String, Class<?>> keyClassMap = LOADER_MAP.get(tClassName);
        if (keyClassMap == null) {
            throw new RuntimeException(String.format("SpiLoader 未加载 %s 类型", tClassName));
        }
        if (!keyClassMap.containsKey(key)) {
            throw new RuntimeException(String.format("SpiLoader 的 %s 不存在 key=%s 的类型", tClassName, key));
        }
        // 获取实现类的 Class 对象
        Class<?> implClass = keyClassMap.get(key);
        // 从实例缓存中获取或创建实例（单例）
        String implClassName = implClass.getName();
        if (!INSTANCE_CACHE.containsKey(implClassName)) {
            try {
                INSTANCE_CACHE.put(implClassName, implClass.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                String errorMsg = String.format("%s 类实例化失败", implClassName);
                throw new RuntimeException(errorMsg, e);
            }
        }
        return (T) INSTANCE_CACHE.get(implClassName);
    }

    /**
     * 加载某个接口类型的所有 SPI 实现 —— "读取插件目录"
     *
     * <p>扫描配置文件，解析 key=value 格式的映射，
     * 将 key 和对应的 Class 存入 LOADER_MAP。
     *
     * <p>扫描顺序：先系统目录，再自定义目录。
     * 如果同一个 key 在两个目录中都有定义，自定义的会覆盖系统的（后写入覆盖先写入）。
     *
     * @param loadClass 要加载的接口类型
     */
    public static void load(Class<?> loadClass) {
        log.info("加载类型为 {} 的 SPI", loadClass.getName());
        // 用户自定义的 SPI 优先级高于系统 SPI（后写入覆盖先写入）
        Map<String, Class<?>> keyClassMap = new HashMap<>();
        for (String scanDir : SCAN_DIRS) {
            List<URL> resources = ResourceUtil.getResources(scanDir + loadClass.getName());
            for (URL resource : resources) {
                try {
                    InputStreamReader inputStreamReader = new InputStreamReader(resource.openStream());
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        // 每行格式：key=全限定类名
                        String[] strArray = line.split("=");
                        if (strArray.length > 1) {
                            String key = strArray[0];
                            String className = strArray[1];
                            keyClassMap.put(key, Class.forName(className));
                        }
                    }
                } catch (Exception e) {
                    log.error("spi resource load error", e);
                }
            }
        }
        LOADER_MAP.put(loadClass.getName(), keyClassMap);
    }
}
