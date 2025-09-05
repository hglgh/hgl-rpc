package com.hgl.example.consumer;

import com.hgl.example.common.model.User;
import com.hgl.example.common.service.UserService;
import com.hgl.hglrpc.bootstrap.ConsumerBootstrap;
import com.hgl.hglrpc.proxy.ServiceProxyFactory;

/**
 * @ClassName: ConsumerExample
 * @Package: com.hgl.example.consumer
 * @Description:
 * @Author HGL
 * @Create: 2025/9/1 11:06
 */
public class ConsumerExample {
    public static void main(String[] args) {
        // 服务提供者初始化
        ConsumerBootstrap.init();
        // 获取代理
        UserService userService = ServiceProxyFactory.getProxy(UserService.class);
        User user = new User();
        user.setName("hgl");
        for (int i = 0; i < 10; i++) {

            // 调用
            User newUser = userService.getUser(user);
            if (newUser != null) {
                System.out.println(newUser.getName());
            } else {
                System.out.println("user == null");
            }
//            long number = userService.getNumber();
//            System.out.println(number);
        }
    }
}
