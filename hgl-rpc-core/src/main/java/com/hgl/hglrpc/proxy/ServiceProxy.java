package com.hgl.hglrpc.proxy;

import cn.hutool.core.collection.CollUtil;
import com.hgl.hglrpc.RpcApplication;
import com.hgl.hglrpc.config.RpcConfig;
import com.hgl.hglrpc.constant.RpcConstant;
import com.hgl.hglrpc.fault.retry.RetryStrategy;
import com.hgl.hglrpc.fault.retry.RetryStrategyFactory;
import com.hgl.hglrpc.fault.tolerant.TolerantStrategy;
import com.hgl.hglrpc.fault.tolerant.TolerantStrategyFactory;
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
        RpcRequest rpcRequest = buildRpcRequest(method, args);
        
        // 获取服务提供者请求元信息
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        List<ServiceMetaInfo> serviceMetaInfoList = getServiceMetaInfoList(rpcRequest, rpcConfig);
        
        // 负载均衡选择服务节点
        ServiceMetaInfo selectedServiceMetaInfo = loadBalanceSelect(rpcRequest, rpcConfig, serviceMetaInfoList);
        
        // 发送请求并获取响应
        RpcResponse rpcResponse = sendRequestWithRetry(rpcRequest, rpcConfig, selectedServiceMetaInfo, serviceMetaInfoList);
        
        // 返回响应结果
        return rpcResponse.getData();
    }
    
    /**
     * 构造RPC请求
     *
     * @param method 方法
     * @param args   参数
     * @return RpcRequest
     */
    private RpcRequest buildRpcRequest(Method method, Object[] args) {
        String serviceName = method.getDeclaringClass().getName();
        return RpcRequest.builder()
                .serviceName(serviceName)
                .methodName(method.getName())
                .parameterTypes(method.getParameterTypes())
                .args(args)
                .build();
    }
    
    /**
     * 获取服务元信息列表
     *
     * @param rpcRequest RPC请求
     * @param rpcConfig  RPC配置
     * @return 服务元信息列表
     */
    private List<ServiceMetaInfo> getServiceMetaInfoList(RpcRequest rpcRequest, RpcConfig rpcConfig) {
        // 从注册中心获取服务提供者请求地址
        Registry registry = RegistryFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName(rpcRequest.getServiceName());
        serviceMetaInfo.setServiceVersion(RpcConstant.DEFAULT_SERVICE_VERSION);
        List<ServiceMetaInfo> serviceMetaInfoList = registry.serviceDiscovery(serviceMetaInfo.getServiceKey());
        if (CollUtil.isEmpty(serviceMetaInfoList)) {
            throw new RuntimeException("暂无服务地址");
        }
        return serviceMetaInfoList;
    }
    
    /**
     * 负载均衡选择服务节点
     *
     * @param rpcRequest          RPC请求
     * @param rpcConfig           RPC配置
     * @param serviceMetaInfoList 服务元信息列表
     * @return 选中的服务元信息
     */
    private ServiceMetaInfo loadBalanceSelect(RpcRequest rpcRequest, RpcConfig rpcConfig, List<ServiceMetaInfo> serviceMetaInfoList) {
        LoadBalancer loadBalancer = LoadBalancerFactory.getInstance(rpcConfig.getLoadBalancer());
        // 将调用方法名（请求路径）作为负载均衡参数
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("methodName", rpcRequest.getMethodName());
        return loadBalancer.select(requestParams, serviceMetaInfoList);
    }
    
    /**
     * 发送请求并支持重试机制
     *
     * @param rpcRequest               RPC请求
     * @param rpcConfig                RPC配置
     * @param selectedServiceMetaInfo 选中的服务元信息
     * @param serviceMetaInfoList      服务元信息列表
     * @return RPC响应
     */
    private RpcResponse sendRequestWithRetry(RpcRequest rpcRequest, RpcConfig rpcConfig, ServiceMetaInfo selectedServiceMetaInfo, List<ServiceMetaInfo> serviceMetaInfoList) {
        try {
            RetryStrategy retryStrategy = RetryStrategyFactory.getInstance(rpcConfig.getRetryStrategy());
            return retryStrategy.doRetry(() -> {
                try {
                    // 发送 TCP/http 请求
                    return VertxClientFactory.getInstance(rpcConfig.getProtocol()).doRequest(rpcRequest, selectedServiceMetaInfo);
                } catch (Throwable e) {
                    throw new RuntimeException("发送请求失败：" + e.getMessage());
                }
            });
        } catch (Exception e) {
            // 容错机制
            return handleTolerance(rpcRequest, rpcConfig, selectedServiceMetaInfo, serviceMetaInfoList, e);
        }
    }
    
    /**
     * 处理容错机制
     *
     * @param rpcRequest               RPC请求
     * @param rpcConfig                RPC配置
     * @param selectedServiceMetaInfo 选中的服务元信息
     * @param serviceMetaInfoList      服务元信息列表
     * @param e                        异常
     * @return RPC响应
     */
    private RpcResponse handleTolerance(RpcRequest rpcRequest, RpcConfig rpcConfig, ServiceMetaInfo selectedServiceMetaInfo, List<ServiceMetaInfo> serviceMetaInfoList, Exception e) {
        TolerantStrategy tolerantStrategy = TolerantStrategyFactory.getInstance(rpcConfig.getTolerantStrategy());
        Map<String, Object> requestTolerantParamMap = new HashMap<>();
        requestTolerantParamMap.put("rpcRequest", rpcRequest);
        requestTolerantParamMap.put("selectedServiceMetaInfo", selectedServiceMetaInfo);
        requestTolerantParamMap.put("serviceMetaInfoList", serviceMetaInfoList);
        return tolerantStrategy.doTolerant(requestTolerantParamMap, e);
    }
}
