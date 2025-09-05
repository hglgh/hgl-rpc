package com.hgl.examplespringbootconsumer;

import com.hgl.example.common.model.User;
import com.hgl.example.common.service.UserService;
import com.hgl.hglrpcspringbootstarter.annotation.RpcReference;
import org.springframework.stereotype.Service;

/**
 * @ClassName: ExampleServiceImpl
 * @Package: com.hgl.examplespringbootconsumer
 * @Description:
 * @Author HGL
 * @Create: 2025/9/5 16:40
 */
@Service
public class ExampleServiceImpl {

    @RpcReference
    private UserService userService;

    public void test() {
        User user = new User();
        user.setName("hgl");
        User resultUser = userService.getUser(user);
        System.out.println(resultUser.getName());
    }
}
