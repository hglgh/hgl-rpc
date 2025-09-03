package com.hgl.hglrpc;

import com.hgl.hglrpc.config.RegistryConfig;
import com.hgl.hglrpc.config.RpcConfig;
import com.hgl.hglrpc.constant.RpcConstant;
import com.hgl.hglrpc.registry.Registry;
import com.hgl.hglrpc.registry.RegistryFactory;
import com.hgl.hglrpc.utils.ConfigUtils;
import lombok.extern.slf4j.Slf4j;

/**
 * @ClassName: RpcApplication
 * @Package: com.hgl.hglrpc
 * @Description: RPC 框架应用 相当于 holder，存放了项目全局用到的变量。双检锁单例模式实现
 * @Author HGL
 * @Create: 2025/9/1 10:47
 */
@Slf4j
public class RpcApplication {
    private static volatile RpcConfig rpcConfig;

    /**
     * 框架初始化，支持传入自定义配置
     *
     * @param newRpcConfig 自定义配置
     */
    private static void init(RpcConfig newRpcConfig) {
        rpcConfig = newRpcConfig;
        log.info("rpc init, config = {}", newRpcConfig.toString());
        // 注册中心初始化
        RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
        Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());
        registry.init(registryConfig);
        log.info("registry init, config = {}", registryConfig);
        if ("provider".equalsIgnoreCase(rpcConfig.getRole())) {
            registry.heartBeat();
            // 创建并注册 Shutdown Hook，JVM 退出时执行操作
            Runtime.getRuntime().addShutdownHook(new Thread(registry::destroy));
        }
    }

    /**
     * 初始化
     */
    private static void init() {
        RpcConfig newRpcConfig;
        try {
            newRpcConfig = ConfigUtils.loadConfig(RpcConfig.class, RpcConstant.DEFAULT_CONFIG_PREFIX);
        } catch (Exception e) {
            // 配置加载失败，使用默认值
            newRpcConfig = new RpcConfig();
            log.error("load config error, use default config");
        }
        init(newRpcConfig);
    }

    /**
     * 获取配置
     *
     * @return 配置
     */
    public static RpcConfig getRpcConfig() {
        if (rpcConfig == null) {
            synchronized (RpcApplication.class) {
                if (rpcConfig == null) {
                    init();
                }
            }
        }
        return rpcConfig;
    }
}

