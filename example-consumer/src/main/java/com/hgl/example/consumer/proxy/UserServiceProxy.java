package com.hgl.example.consumer.proxy;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.hgl.example.common.model.User;
import com.hgl.example.common.service.UserService;
import com.hgl.hglrpc.model.RpcRequest;
import com.hgl.hglrpc.model.RpcResponse;
import com.hgl.hglrpc.serializer.JdkSerializer;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @ClassName: UserServiceProxy
 * @Package: com.hgl.example.consumer
 * @Description: 静态代理
 * @Author HGL
 * @Create: 2025/8/29 17:00
 */
@Slf4j
public class UserServiceProxy implements UserService {
    @Override
    public User getUser(User user) {
        // 指定序列化器
        JdkSerializer jdkSerializer = new JdkSerializer();
        // 发请求
        RpcRequest rpcRequest = RpcRequest.builder()
                .serviceName(UserService.class.getName())
                .methodName("getUser")
                .parameterTypes(new Class[]{User.class})
                .args(new Object[]{user})
                .build();
        try {
            byte[] bytes = jdkSerializer.serialize(rpcRequest);
            byte[] result;
            try (HttpResponse httpResponse = HttpRequest.post("http://localhost:8080").body(bytes).execute()) {
                result = httpResponse.bodyBytes();
            }
            RpcResponse rpcResponse = jdkSerializer.deserialize(result, RpcResponse.class);
            return (User) rpcResponse.getData();
        } catch (IOException e) {
            log.error("Error: ", e);
        }
        return null;
    }
}
