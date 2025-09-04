package com.hgl.example.consumer.proxy;

import cn.hutool.core.collection.CollUtil;
import com.hgl.hglrpc.RpcApplication;
import com.hgl.hglrpc.config.RpcConfig;
import com.hgl.hglrpc.constant.RpcConstant;
import com.hgl.hglrpc.loadbalancer.LoadBalancer;
import com.hgl.hglrpc.loadbalancer.LoadBalancerFactory;
import com.hgl.hglrpc.model.RpcRequest;
import com.hgl.hglrpc.model.RpcResponse;
import com.hgl.hglrpc.model.ServiceMetaInfo;
import com.hgl.hglrpc.registry.Registry;
import com.hgl.hglrpc.registry.RegistryFactory;
import com.hgl.hglrpc.server.client.VertxClientFactory;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: ServiceProxy
 * @Package: com.hgl.example.consumer.proxy
 * @Description: 服务代理（JDK 动态代理）
 * @Author HGL
 * @Create: 2025/8/29 17:08
 */
@Slf4j
public class ServiceProxy implements InvocationHandler {
    /**
     * 调用代理
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // 构造请求
        String serviceName = method.getDeclaringClass().getName();
        RpcRequest rpcRequest = RpcRequest.builder()
                .serviceName(serviceName)
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .args(args)
                .build();
        // 获取服务提供者请求元信息
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        ServiceMetaInfo selectedServiceMetaInfo = getSelectedServiceMetaInfo(rpcRequest, rpcConfig, serviceName);
        // 发送 TCP 请求
        RpcResponse rpcResponse = VertxClientFactory.getInstance(rpcConfig.getProtocol()).doRequest(rpcRequest, selectedServiceMetaInfo);
        return rpcResponse.getData();

    }

    /**
     * 获取服务提供者请求元信息
     *
     * @param serviceName 服务名称
     * @return 请求元信息
     */
    private static ServiceMetaInfo getSelectedServiceMetaInfo(RpcRequest rpcRequest, RpcConfig rpcConfig, String serviceName) {
        // 从注册中心获取服务提供者请求地址
        Registry registry = RegistryFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName(serviceName);
        serviceMetaInfo.setServiceVersion(RpcConstant.DEFAULT_SERVICE_VERSION);
        List<ServiceMetaInfo> serviceMetaInfoList = registry.serviceDiscovery(serviceMetaInfo.getServiceKey());
        if (CollUtil.isEmpty(serviceMetaInfoList)) {
            throw new RuntimeException("暂无服务地址");
        }
        LoadBalancer loadBalancer = LoadBalancerFactory.getInstance(rpcConfig.getLoadBalancer());
        // 将调用方法名（请求路径）作为负载均衡参数
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("methodName", rpcRequest.getMethodName());
        return loadBalancer.select(requestParams, serviceMetaInfoList);
    }
}
