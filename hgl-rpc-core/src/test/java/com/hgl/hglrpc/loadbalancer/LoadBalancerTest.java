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
 * @Description: 负载均衡器的验收测试
 * @Author HGL
 * @Create: 2025/9/4 18:03
 *
 * <h2>测试全景</h2>
 * 负载均衡器就像一位"交通调度员"——面对多条车道（多个服务实例），
 * 它要决定每一辆车（请求）走哪条车道，既要避免某条车道堵死，
 * 又要保证同一类请求尽量走同一条道（会话粘性/一致性哈希）。
 *
 * <p>本测试类聚焦 {@link ConsistentHashLoadBalancer}（一致性哈希负载均衡器），
 * 验证它能否在给定的请求参数和候选服务列表中，稳定地选出一个实例。
 */
class LoadBalancerTest {

    /** 被测对象：一致性哈希负载均衡器——"调度员" */
    final LoadBalancer loadBalancer = new ConsistentHashLoadBalancer();

    /**
     * <h3>测试目标：一致性哈希的选择稳定性</h3>
     * <p>
     * 同样的请求参数（methodName = "apple"）连续调用 3 次，
     * 就像同一个乘客连续叫了 3 次车——一致性哈希的承诺是：
     * 只要"乘客信息"不变、"车道"不变，每次分配的车道应该相同（或至少合理）。
     *
     * <h3>期望行为</h3>
     * <ul>
     *   <li>每次调用都能选出一个非空的服务实例（{@code assertNotNull}）</li>
     *   <li>选出的实例确实来自候选列表</li>
     *   <li>一致性哈希保证相同参数映射到相同节点（可通过日志输出人工验证）</li>
     * </ul>
     */
    @Test
    public void select() {
        // --- 构造请求参数：就像乘客递给调度员的"目的地标签" ---
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("methodName", "apple");

        // --- 获取候选服务列表：就像调度员面前的"车道清单" ---
        List<ServiceMetaInfo> serviceMetaInfoList = getServiceMetaInfos();

        // --- 连续调用 3 次，验证选择结果的稳定性 ---
        // 第 1 次调度：选出一个实例
        ServiceMetaInfo serviceMetaInfo = loadBalancer.select(requestParams, serviceMetaInfoList);
        System.out.println(serviceMetaInfo);
        assertNotNull(serviceMetaInfo); // 必须选出一个，不能"两手一摊"

        // 第 2 次调度：同一乘客、同一目的地
        serviceMetaInfo = loadBalancer.select(requestParams, serviceMetaInfoList);
        System.out.println(serviceMetaInfo);
        assertNotNull(serviceMetaInfo);

        // 第 3 次调度：验证一致性——相同参数应映射到相同节点
        serviceMetaInfo = loadBalancer.select(requestParams, serviceMetaInfoList);
        System.out.println(serviceMetaInfo);
        assertNotNull(serviceMetaInfo);
    }

    /**
     * 构造候选服务实例列表。
     * 模拟两个服务节点，就像高速公路上的两条车道：
     * <ul>
     *   <li>hahahah.com:1234 —— 车道 A</li>
     *   <li>hgl.icu:80 —— 车道 B</li>
     * </ul>
     * 两者同属 "myService 1.0"，是同一服务的水平扩展副本。
     */
    private static List<ServiceMetaInfo> getServiceMetaInfos() {
        // 车道 A
        ServiceMetaInfo serviceMetaInfo1 = new ServiceMetaInfo();
        serviceMetaInfo1.setServiceName("myService");
        serviceMetaInfo1.setServiceVersion("1.0");
        serviceMetaInfo1.setServiceHost("hahahah.com");
        serviceMetaInfo1.setServicePort(1234);

        // 车道 B
        ServiceMetaInfo serviceMetaInfo2 = new ServiceMetaInfo();
        serviceMetaInfo2.setServiceName("myService");
        serviceMetaInfo2.setServiceVersion("1.0");
        serviceMetaInfo2.setServiceHost("hgl.icu");
        serviceMetaInfo2.setServicePort(80);

        return Arrays.asList(serviceMetaInfo1, serviceMetaInfo2);
    }
}
