package com.hgl.hglrpc.config;

import com.hgl.hglrpc.loadbalancer.LoadBalancerKeys;
import com.hgl.hglrpc.serializer.SerializerKeys;
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

    /**
     * 序列化器
     */
    private String serializer = SerializerKeys.JDK;

    /**
     * 角色
     */
    private String role = "provider";

    /**
     * 传输协议
     */
    private String protocol = "tcp";

    /**
     * 负载均衡器
     */
    private String loadBalancer = LoadBalancerKeys.ROUND_ROBIN;

    /**
     * 注册中心配置
     */
    private RegistryConfig registryConfig = new RegistryConfig();
}
