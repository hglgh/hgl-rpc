package com.hgl.examplespringbootprovider;

import com.hgl.example.common.model.User;
import com.hgl.example.common.service.UserService;
import com.hgl.hglrpcspringbootstarter.annotation.RpcService;
import org.springframework.stereotype.Service;

/**
 * @ClassName: UserServiceImpl
 * @Package: com.hgl.examplespringbootprovider
 * @Description: 用户服务实现类
 * @Author HGL
 * @Create: 2025/9/5 16:38
 */
@Service
@RpcService
public class UserServiceImpl implements UserService {
    @Override
    public User getUser(User user) {
        System.out.println("用户名：" + user.getName());
        return user;
    }
}
