package com.hgl.hglrpc.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Dict;
import cn.hutool.core.util.StrUtil;
import cn.hutool.setting.dialect.Props;
import cn.hutool.setting.yaml.YamlUtil;

/**
 * @ClassName: ConfigUtils
 * @Package: com.hgl.hglrpc.utils
 * @Description: 配置工具类
 * @Author HGL
 * @Create: 2025/9/1 10:36
 */
public class ConfigUtils {

    // 支持的配置文件扩展名
    private static final String[] SUPPORTED_EXTENSIONS = {".properties", ".yml", ".yaml"};

    /**
     * 加载配置
     *
     * @param clazz  配置类
     * @param prefix 前缀
     * @param <T>    配置类泛型
     * @return 配置类实例
     */
    public static <T> T loadConfig(Class<T> clazz, String prefix) {
        return loadConfig(clazz, prefix, "");
    }

    /**
     * 加载配置对象，支持区分环境
     *
     * @param clazz       配置类
     * @param prefix      前缀
     * @param environment 环境
     * @param <T>         配置类泛型
     * @return 配置类实例
     */
    private static <T> T loadConfig(Class<T> clazz, String prefix, String environment) {
        StringBuilder fileNameBuilder = new StringBuilder("application");
        if (StrUtil.isNotBlank(environment)) {
            fileNameBuilder.append("-").append(environment);
        }

        // 查找存在的配置文件
        String configFile = findConfigFile(fileNameBuilder.toString());
        if (configFile == null) {
            throw new RuntimeException("No configuration file found for: " + fileNameBuilder);
        }

        // 根据文件扩展名选择加载方式
        if (configFile.endsWith(".properties")) {
            Props props = new Props(configFile);
            return props.toBean(clazz, prefix);
        } else {
            // 处理YAML/YML文件
            return loadFromYaml(configFile, clazz, prefix);
        }
    }

    /**
     * 从YAML文件加载配置
     *
     * @param configFile 配置文件路径
     * @param clazz      配置类
     * @param prefix     前缀
     * @param <T>        配置类泛型
     * @return 配置类实例
     */
    private static <T> T loadFromYaml(String configFile, Class<T> clazz, String prefix) {
        Dict dict = YamlUtil.loadByPath(configFile);
        return BeanUtil.toBean(dict.get(prefix), clazz);
    }

    /**
     * 查找存在的配置文件
     *
     * @param fileName 不含扩展名的文件名
     * @return 完整的配置文件路径，如果找不到则返回null
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
