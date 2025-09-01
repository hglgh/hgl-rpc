package com.hgl.hglrpc.config;

import lombok.Data;

/**
 * @ClassName: RpcConfig
 * @Package: com.hgl.hglrpc.config
 * @Description: RPC 框架配置
 * @Author HGL
 * @Create: 2025/9/1 10:33
 */
@Data
public class RpcConfig {

    /**
     * 名称
     */
    private String name = "hgl-rpc";

    /**
     * 版本号
     */
    private String version = "1.0";

    /**
     * 服务器主机名
     */
    private String serverHost = "localhost";

    /**
     * 服务器端口号
     */
    private Integer serverPort = 8080;

    /**
     * 模拟调用
     */
    private boolean mock = false;
}
