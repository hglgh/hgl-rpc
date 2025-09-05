package com.hgl.hglrpc.bootstrap;

import com.hgl.hglrpc.RpcApplication;

/**
 * @ClassName: ConsumerBootstrap
 * @Package: com.hgl.hglrpc.bootstrap
 * @Description: 服务消费者启动类（初始化）
 * @Author HGL
 * @Create: 2025/9/5 15:57
 */
public class ConsumerBootstrap {

    /**
     * 初始化
     */
    public static void init() {
        // RPC 框架初始化（配置和注册中心）
        RpcApplication.init();
    }
}
