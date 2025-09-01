package com.hgl.example.consumer;

import com.hgl.example.common.model.User;
import com.hgl.example.common.service.UserService;
import com.hgl.example.consumer.proxy.ServiceProxyFactory;
import com.hgl.hglrpc.config.RpcConfig;
import com.hgl.hglrpc.constant.RpcConstant;
import com.hgl.hglrpc.utils.ConfigUtils;

/**
 * @ClassName: ConsumerExample
 * @Package: com.hgl.example.consumer
 * @Description:
 * @Author HGL
 * @Create: 2025/9/1 11:06
 */
public class ConsumerExample {
    public static void main(String[] args) {
//        RpcConfig rpcConfig = ConfigUtils.loadConfig(RpcConfig.class, RpcConstant.DEFAULT_CONFIG_PREFIX);
//        System.out.println(rpcConfig);
        // 获取代理
        UserService userService = ServiceProxyFactory.getProxy(UserService.class);
        User user = new User();
        user.setName("hgl");
        // 调用
        User newUser = userService.getUser(user);
        if (newUser != null) {
            System.out.println(newUser.getName());
        } else {
            System.out.println("user == null");
        }
        long number = userService.getNumber();
        System.out.println(number);
    }
}
