package com.hgl.example.consumer;

import com.hgl.example.common.model.User;
import com.hgl.example.common.service.UserService;
import com.hgl.example.consumer.proxy.UserServiceProxy;

/**
 * @ClassName: EasyConsumerExample
 * @Package: com.hgl.example.consumer
 * @Description: 简易服务消费者示例
 * @Author HGL
 * @Create: 2025/8/29 15:03
 */
public class EasyConsumerExample {
    public static void main(String[] args) {
        // 静态代理
        UserService userService = new UserServiceProxy();
        // 动态代理
//        UserService userService = ServiceProxyFactory.getProxy(UserService.class);
        User user = new User();
        user.setName("hgl");
        // 调用
        User newUser = userService.getUser(user);
        if (newUser != null) {
            System.out.println(newUser.getName());
        } else {
            System.out.println("user == null");
        }
    }
}
