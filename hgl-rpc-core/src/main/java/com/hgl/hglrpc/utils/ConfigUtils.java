package com.hgl.hglrpc.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.dialect.Props;
import cn.hutool.setting.yaml.YamlUtil;

/**
 * 配置工具类 —— "配置文件的万能读取器"
 *
 * <p>负责从配置文件中读取配置，并映射到 Java 对象中。
 * 支持两种配置文件格式：
 * <pre>
 *   1. .properties 格式（传统 Java 配置格式）
 *      rpc.name=hgl-rpc
 *      rpc.serverPort=8080
 *
 *   2. .yml / .yaml 格式（更现代的配置格式）
 *      rpc:
 *        name: hgl-rpc
 *        serverPort: 8080
 * </pre>
 *
 * <p>加载优先级：先找 application.properties，找不到再找 application.yml，
 * 再找不到找 application.yaml。找到第一个就用。
 *
 * @Author HGL
 * @Create: 2025/9/1 10:36
 */
public class ConfigUtils {

    /** 支持的配置文件扩展名（按优先级排列） */
    private static final String[] SUPPORTED_EXTENSIONS = {".properties", ".yml", ".yaml"};

    /**
     * 加载配置对象（无环境区分）
     *
     * @param clazz  配置类（如 RpcConfig.class）
     * @param prefix 配置前缀（如 "rpc"）
     * @param <T>    配置类泛型
     * @return 填充好字段值的配置对象
     */
    public static <T> T loadConfig(Class<T> clazz, String prefix) {
        return loadConfig(clazz, prefix, "");
    }

    /**
     * 加载配置对象（支持区分环境，如 application-dev.yml）
     *
     * @param clazz       配置类
     * @param prefix      配置前缀
     * @param environment 环境名称（如 "dev", "prod"）
     * @param <T>         配置类泛型
     * @return 填充好字段值的配置对象
     */
    private static <T> T loadConfig(Class<T> clazz, String prefix, String environment) {
        StringBuilder fileNameBuilder = new StringBuilder("application");
        if (StrUtil.isNotBlank(environment)) {
            fileNameBuilder.append("-").append(environment);
        }

        // 在 classpath 中查找存在的配置文件
        String configFile = findConfigFile(fileNameBuilder.toString());
        if (configFile == null) {
            throw new RuntimeException("No configuration file found for: " + fileNameBuilder);
        }

        // 根据文件扩展名选择不同的加载方式
        if (configFile.endsWith(".properties")) {
            Props props = new Props(configFile);
            return props.toBean(clazz, prefix);
        } else {
            return loadFromYaml(configFile, clazz, prefix);
        }
    }

    /**
     * 从 YAML 文件加载配置
     *
     * <p>先把 YAML 解析成 Dict（类似 Map），再根据 prefix 取出对应的子树，
     * 最后用 BeanUtil 转成目标配置类对象。
     */
    private static <T> T loadFromYaml(String configFile, Class<T> clazz, String prefix) {
        Dict dict = YamlUtil.loadByPath(configFile);
        return BeanUtil.toBean(dict.get(prefix), clazz);
    }

    /**
     * 在 classpath 中查找存在的配置文件
     *
     * @param fileName 不含扩展名的文件名（如 "application"）
     * @return 找到的第一个配置文件完整路径，找不到返回 null
     */
    private static String findConfigFile(String fileName) {
        for (String extension : SUPPORTED_EXTENSIONS) {
            String fullName = fileName + extension;
            if (FileUtil.exist(fullName)) {
                return fullName;
            }
        }
        return null;
    }
}
