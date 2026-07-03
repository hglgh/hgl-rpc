package com.hgl.hglrpc.constant;

/**
 * RPC 相关常量 —— "全局统一的规矩"
 *
 * <p>定义了 RPC 框架中到处使用的常量值，
 * 避免在代码中出现"魔法字符串"（magic string）。
 *
 * @Author HGL
 * @Create: 2025/9/1 10:45
 */
public interface RpcConstant {
    /**
     * 配置文件中 RPC 配置的前缀
     *
     * <p>对应 application.yml 中的 "rpc:" 层级。
     * ConfigUtils.loadConfig(RpcConfig.class, "rpc") 会读取这个前缀下的所有配置。
     */
    String DEFAULT_CONFIG_PREFIX = "rpc";

    /**
     * 默认服务版本号
     *
     * <p>当 RpcRequest 中未指定版本时，使用此默认值。
     * 确保版本号为 null 不会导致注册中心查找失败。
     */
    String DEFAULT_SERVICE_VERSION = "1.0";
}
