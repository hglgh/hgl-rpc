package com.hgl.example.provider;

import com.hgl.example.common.service.UserService;
import com.hgl.hglrpc.registry.LocalRegistry;
import com.hgl.hglrpc.server.http.VertxHttpServer;

/**
 * @ClassName: EasyProviderExample
 * @Package: com.hgl.example
 * @Description: 简易服务提供者示例
 * @Author HGL
 * @Create: 2025/8/29 14:55
 */
public class EasyProviderExample {
    public static void main(String[] args) {
        // 注册服务
        LocalRegistry.register(UserService.class.getName(), UserServiceImpl.class);
        // 启动 web 服务
        new VertxHttpServer().doStart(8080);
    }
}
