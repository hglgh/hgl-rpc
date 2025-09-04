package com.hgl.hglrpc.loadbalancer;

import com.hgl.hglrpc.model.ServiceMetaInfo;

import java.util.List;
import java.util.Map;

/**
 * @ClassName: LoadBalancer
 * @Package: com.hgl.hglrpc.loadbalancer
 * @Description: 负载均衡器（消费端使用）
 * @Author HGL
 * @Create: 2025/9/4 17:43
 */
public interface LoadBalancer {
    /**
     * 选择服务调用
     *
     * @param requestParams       请求参数
     * @param serviceMetaInfoList 可用服务列表
     * @return 服务元信息
     */
    ServiceMetaInfo select(Map<String, Object> requestParams, List<ServiceMetaInfo> serviceMetaInfoList);
}
