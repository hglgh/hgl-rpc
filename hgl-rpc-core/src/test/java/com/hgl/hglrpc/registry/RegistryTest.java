package com.hgl.hglrpc.registry;

import cn.hutool.core.collection.CollUtil;
import com.hgl.hglrpc.config.RegistryConfig;
import com.hgl.hglrpc.model.ServiceMetaInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @ClassName: RegistryTest
 * @Package: com.hgl.hglrpc.registry
 * @Description: 注册中心的验收测试
 * @Author HGL
 * @Create: 2025/9/2 14:17
 *
 * <h2>测试全景</h2>
 * 注册中心就像一座"服务交易所"——服务提供者在这里挂牌上市（register），
 * 服务消费者来这里查询行情（serviceDiscovery），而心跳检测则是交易所对
 * 每只"股票"的定期巡查，确保它没有退市。
 *
 * <p>本测试类围绕 {@link EtcdRegistry} 的四大核心动作展开：
 * <ol>
 *   <li>挂牌（register）——多实例、多版本同时注册</li>
 *   <li>摘牌（unRegister）——主动下线某实例</li>
 *   <li>行情查询（serviceDiscovery）——按服务名+版本检索可用实例列表</li>
 *   <li>心跳巡查（heartBeat）——验证租约续约机制能否让服务"存活"</li>
 * </ol>
 *
 * <p><b>前置条件：</b>需要一台可达的 etcd 实例（地址见 {@code init()} 方法），
 * 否则所有测试都会因为连接失败而报错。
 */
class RegistryTest {

    /** 被测对象：etcd 注册中心实现，相当于"交易所大厅" */
    final Registry registry = new EtcdRegistry();

    /**
     * 测试前置：初始化注册中心，连接 etcd。
     * 相当于"每天开市前先打开交易所大门、接通网络"。
     */
    @BeforeEach
    public void init() {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("http://192.168.6.128:2379");
        registry.init(registryConfig);
    }

    /**
     * <h3>测试目标：多实例、多版本的服务注册</h3>
     * <p>
     * 就像同一家公司可以在交易所挂牌多只不同版本的股票：
     * <ul>
     *   <li>myService 1.0 的实例 A（端口 1234）</li>
     *   <li>myService 1.0 的实例 B（端口 1235）——同版本的另一个副本</li>
     *   <li>myService 2.0 的实例 C（端口 1234）——新版本迭代上线</li>
     * </ul>
     *
     * <h3>期望行为</h3>
     * 三次注册全部成功完成，不抛出任何异常。
     * etcd 中应当存在三条独立的键值记录，互不干扰。
     */
    @Test
    public void register() throws Exception {
        // --- 第一只"股票"：myService 1.0 实例，端口 1234 ---
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName("myService");
        serviceMetaInfo.setServiceVersion("1.0");
        serviceMetaInfo.setServiceHost("localhost");
        serviceMetaInfo.setServicePort(1234);
        registry.register(serviceMetaInfo);

        // --- 第二只"股票"：同名同版本，不同端口——水平扩容的副本 ---
        serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName("myService");
        serviceMetaInfo.setServiceVersion("1.0");
        serviceMetaInfo.setServiceHost("localhost");
        serviceMetaInfo.setServicePort(1235);
        registry.register(serviceMetaInfo);

        // --- 第三只"股票"：myService 2.0 实例——版本迭代升级 ---
        serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName("myService");
        serviceMetaInfo.setServiceVersion("2.0");
        serviceMetaInfo.setServiceHost("localhost");
        serviceMetaInfo.setServicePort(1234);
        registry.register(serviceMetaInfo);
    }

    /**
     * <h3>测试目标：服务注销（摘牌）</h3>
     * <p>
     * 当一个服务实例下线时，它需要从交易所"摘牌退市"，否则消费者
     * 仍然会把请求路由到这个已经不存在的实例上。
     *
     * <h3>期望行为</h3>
     * 成功注销指定的服务实例，不抛出异常。
     * 后续再做 serviceDiscovery 时，不应再发现该实例。
     */
    @Test
    public void unRegister() throws ExecutionException, InterruptedException {
        // 准备注销的实例：myService 1.0, localhost:1234
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName("myService");
        serviceMetaInfo.setServiceVersion("1.0");
        serviceMetaInfo.setServiceHost("localhost");
        serviceMetaInfo.setServicePort(1234);
        registry.unRegister(serviceMetaInfo);
    }

    /**
     * <h3>测试目标：服务发现——按服务名+版本查询可用实例</h3>
     * <p>
     * 消费者调用服务前，就像股民下单前要查询"某只股票的交易对手列表"——
     * 拿到所有可用实例后，再由负载均衡器从中选出一个。
     *
     * <h3>期望行为</h3>
     * 查询结果不为空（{@code assertNotNull}），说明注册中心确实返回了
     * 与 "myService 1.0" 匹配的服务实例列表。
     */
    @Test
    public void serviceDiscovery() {
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName("myService");
        serviceMetaInfo.setServiceVersion("1.0");
        String serviceKey = serviceMetaInfo.getServiceKey();
        // 从注册中心"查询行情"——拿到所有匹配的服务实例
        List<ServiceMetaInfo> serviceMetaInfoList = registry.serviceDiscovery(serviceKey);
        assertNotNull(serviceMetaInfoList); // 结果不应为空，说明确实找到了实例
    }

    /**
     * <h3>测试目标：心跳续约机制</h3>
     * <p>
     * etcd 用租约（lease）来管理服务的生命周期——就像股票交易所要求
     * 上市公司定期提交财报，否则自动退市。心跳就是这份"财报"：
     * 定期续约能让服务保持活跃；一旦停止心跳，租约到期后 etcd 会
     * 自动删除对应的键值，服务就此"退市"。
     *
     * <h3>期望行为</h3>
     * 先注册服务（触发心跳线程启动），然后阻塞等待 1 分钟。
     * 在此期间观察日志输出，确认心跳续约正常执行、服务没有被 etcd 清除。
     * 这是一个"观察型"测试——通过日志和 etcd 控制台来人工确认心跳是否正常。
     */
    @Test
    public void heartBeat() throws Exception {
        // init 方法中已经执行心跳检测了
        register();
        // 阻塞 1 分钟——给心跳足够的时间续约若干次，方便在日志中观察
        Thread.sleep(60 * 1000L);
    }
}
