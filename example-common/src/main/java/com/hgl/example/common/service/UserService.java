package com.hgl.example.common.service;

import com.hgl.example.common.model.User;

/**
 * @ClassName: UserService
 * @Package: com.hgl.example.common.service
 * @Description:
 * @Author HGL
 * @Create: 2025/8/29 14:43
 */
public interface UserService {

    /**
     * 获取用户信息
     */
    User getUser(User user);
}
