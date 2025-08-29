package com.hgl.example.provider;

import com.hgl.example.common.model.User;
import com.hgl.example.common.service.UserService;

/**
 * @ClassName: UserServiceImpl
 * @Package: com.hgl.example.provider
 * @Description:
 * @Author HGL
 * @Create: 2025/8/29 14:54
 */
public class UserServiceImpl implements UserService {
    @Override
    public User getUser(User user) {
        System.out.println("用户名：" + user.getName());
        return user;
    }
}
