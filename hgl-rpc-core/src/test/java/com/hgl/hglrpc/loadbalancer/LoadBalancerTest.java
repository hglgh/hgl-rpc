package com.hgl.hglrpc.loadbalancer;

import com.hgl.hglrpc.model.ServiceMetaInfo;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @ClassName: LoadBalancerTest
 * @Package: com.hgl.hglrpc.loadbalancer
 * @Description: 负载均衡器测试
 * @Author HGL
 * @Create: 2025/9/4 18:03
 */
class LoadBalancerTest {
    final LoadBalancer loadBalancer = new ConsistentHashLoadBalancer();

    @Test
    public void select() {
        // 请求参数
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("methodName", "apple");
        // 服务列表
        List<ServiceMetaInfo> serviceMetaInfoList = getServiceMetaInfos();
        // 连续调用 3 次
        ServiceMetaInfo serviceMetaInfo = loadBalancer.select(requestParams, serviceMetaInfoList);
        System.out.println(serviceMetaInfo);
        assertNotNull(serviceMetaInfo);
        serviceMetaInfo = loadBalancer.select(requestParams, serviceMetaInfoList);
        System.out.println(serviceMetaInfo);
        assertNotNull(serviceMetaInfo);
        serviceMetaInfo = loadBalancer.select(requestParams, serviceMetaInfoList);
        System.out.println(serviceMetaInfo);
        assertNotNull(serviceMetaInfo);
    }

    private static List<ServiceMetaInfo> getServiceMetaInfos() {
        ServiceMetaInfo serviceMetaInfo1 = new ServiceMetaInfo();
        serviceMetaInfo1.setServiceName("myService");
        serviceMetaInfo1.setServiceVersion("1.0");
        serviceMetaInfo1.setServiceHost("hahahah.com");
        serviceMetaInfo1.setServicePort(1234);
        ServiceMetaInfo serviceMetaInfo2 = new ServiceMetaInfo();
        serviceMetaInfo2.setServiceName("myService");
        serviceMetaInfo2.setServiceVersion("1.0");
        serviceMetaInfo2.setServiceHost("hgl.icu");
        serviceMetaInfo2.setServicePort(80);
        return Arrays.asList(serviceMetaInfo1, serviceMetaInfo2);
    }
}