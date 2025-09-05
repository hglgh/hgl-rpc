package com.hgl.example.provider;

import com.hgl.example.common.service.UserService;
import com.hgl.hglrpc.bootstrap.ProviderBootstrap;
import com.hgl.hglrpc.model.ServiceRegisterInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * @ClassName: ProviderExample
 * @Package: com.hgl.example.provider
 * @Description:
 * @Author HGL
 * @Create: 2025/9/1 11:07
 */
public class ProviderExample {
    public static void main(String[] args) {
        // 要注册的服务
        List<ServiceRegisterInfo<?>> serviceRegisterInfoList = new ArrayList<>();
        ServiceRegisterInfo<UserService> serviceRegisterInfo = new ServiceRegisterInfo<>(UserService.class.getName(), UserServiceImpl.class);
        serviceRegisterInfoList.add(serviceRegisterInfo);

        // 服务提供者初始化
        ProviderBootstrap.init(serviceRegisterInfoList);
    }

}
