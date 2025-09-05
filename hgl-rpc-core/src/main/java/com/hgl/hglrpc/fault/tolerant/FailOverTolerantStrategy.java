package com.hgl.hglrpc.fault.tolerant;

import cn.hutool.core.collection.CollUtil;
import com.hgl.hglrpc.RpcApplication;
import com.hgl.hglrpc.config.RpcConfig;
import com.hgl.hglrpc.fault.retry.RetryStrategy;
import com.hgl.hglrpc.fault.retry.RetryStrategyFactory;
import com.hgl.hglrpc.loadbalancer.LoadBalancer;
import com.hgl.hglrpc.loadbalancer.LoadBalancerFactory;
import com.hgl.hglrpc.model.RpcRequest;
import com.hgl.hglrpc.model.RpcResponse;
import com.hgl.hglrpc.model.ServiceMetaInfo;
import com.hgl.hglrpc.server.client.VertxClient;
import com.hgl.hglrpc.server.client.VertxClientFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ClassName: FailOverTolerantStrategy
 * @Package: com.hgl.hglrpc.fault.tolerant
 * @Description:
 * @Author HGL
 * @Create: 2025/9/5 14:35
 */

public class FailOverTolerantStrategy implements TolerantStrategy {
    @Override
    @SuppressWarnings("unchecked")
    public RpcResponse doTolerant(Map<String, Object> context, Exception e) {
        //获取其它节点并调用
        RpcRequest rpcRequest = (RpcRequest) context.get("rpcRequest");
        List<ServiceMetaInfo> serviceMetaInfoList = (List<ServiceMetaInfo>) context.get("serviceMetaInfoList");
        ServiceMetaInfo selectedServiceMetaInfo = (ServiceMetaInfo) context.get("selectedServiceMetaInfo");

        //移除失败节点
        removeFailNode(selectedServiceMetaInfo, serviceMetaInfoList);

        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        LoadBalancer loadBalancer = LoadBalancerFactory.getInstance(rpcConfig.getLoadBalancer());
        Map<String, Object> requestParamMap = new HashMap<>();
        requestParamMap.put("methodName", rpcRequest.getMethodName());

        RpcResponse rpcResponse;
        while (!serviceMetaInfoList.isEmpty()) {
            ServiceMetaInfo currentServiceMetaInfo = loadBalancer.select(requestParamMap, serviceMetaInfoList);
            System.out.println("获取节点：" + currentServiceMetaInfo);
            try {
                //发送tcp请求
                RetryStrategy retryStrategy = RetryStrategyFactory.getInstance(rpcConfig.getRetryStrategy());
                VertxClient vertxClient = VertxClientFactory.getInstance(rpcConfig.getProtocol());
                rpcResponse = retryStrategy.doRetry(() -> {
                    try {
                        return vertxClient.doRequest(rpcRequest, currentServiceMetaInfo);
                    } catch (Throwable ex) {
                        throw new RuntimeException(ex);
                    }
                });
                return rpcResponse;
            } catch (Exception exception) {
                //移除失败节点
                removeFailNode(currentServiceMetaInfo, serviceMetaInfoList);
            }
        }
        //调用失败
        throw new RuntimeException(e);
    }

    /**
     * 移除失败节点，可考虑下线
     *
     * @param serviceMetaInfoList 节点列表
     */
    private void removeFailNode(ServiceMetaInfo currentServiceMetaInfo, List<ServiceMetaInfo> serviceMetaInfoList) {
        if (CollUtil.isNotEmpty(serviceMetaInfoList)) {
            serviceMetaInfoList.removeIf(next -> currentServiceMetaInfo.getServiceNodeKey().equals(next.getServiceNodeKey()));
        }
    }
}
