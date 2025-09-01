package com.hgl.example.provider;

import com.hgl.example.common.service.UserService;
import com.hgl.hglrpc.RpcApplication;
import com.hgl.hglrpc.registry.LocalRegistry;
import com.hgl.hglrpc.server.VertxHttpServer;

/**
 * @ClassName: ProviderExample
 * @Package: com.hgl.example.provider
 * @Description:
 * @Author HGL
 * @Create: 2025/9/1 11:07
 */
public class ProviderExample {
    public static void main(String[] args) {

        // 注册服务
        LocalRegistry.register(UserService.class.getName(), UserServiceImpl.class);

        // 启动 web 服务
        new VertxHttpServer().doStart(RpcApplication.getRpcConfig().getServerPort());
    }
}
