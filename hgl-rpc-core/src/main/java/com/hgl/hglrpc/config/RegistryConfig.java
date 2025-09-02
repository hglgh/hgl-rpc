package com.hgl.hglrpc.config;

import lombok.Data;

/**
 * @ClassName: RegistryConfig
 * @Package: com.hgl.hglrpc.config
 * @Description: RPC 框架注册中心配置
 * @Author HGL
 * @Create: 2025/9/2 13:48
 */
@Data
public class RegistryConfig {

    /**
     * 注册中心类别
     */
    private String registry = "etcd";

    /**
     * 注册中心地址
     */
    private String address = "http://192.168.6.128:2379";

    /**
     * 用户名
     */
    private String username;

    /**
     * 密码
     */
    private String password;

    /**
     * 超时时间（单位毫秒）
     */
    private Long timeout = 10000L;
}
