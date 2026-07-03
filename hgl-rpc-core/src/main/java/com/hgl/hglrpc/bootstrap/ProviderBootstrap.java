package com.hgl.hglrpc.bootstrap;

import com.hgl.hglrpc.RpcApplication;
import com.hgl.hglrpc.config.RegistryConfig;
import com.hgl.hglrpc.config.RpcConfig;
import com.hgl.hglrpc.model.ServiceMetaInfo;
import com.hgl.hglrpc.model.ServiceRegisterInfo;
import com.hgl.hglrpc.registry.LocalRegistry;
import com.hgl.hglrpc.registry.Registry;
import com.hgl.hglrpc.registry.RegistryFactory;
import com.hgl.hglrpc.server.VertxServerFactory;

import java.util.List;

/**
 * 提供者的启动流程（ProviderBootstrap）
 *
 * <p>如果说消费者出门采购前要"带好钱包查好路"，
 * 那提供者开店前要做的事情就更多了——
 * 你得把店铺装修好、把商品摆上架、在黄页上登记地址、最后才开门迎客。</p>
 *
 * <p>ProviderBootstrap 就是商家的"开店准备工作"，它要完成几件大事：</p>
 *
 * <pre>
 *   提供者的启动之旅（开店流程）：
 *
 *   ┌──────────────────────────────────────────────────────────┐
 *   │              ProviderBootstrap.init()                     │
 *   └──────────────────────┬───────────────────────────────────┘
 *                          │
 *        ┌─────────────────┼─────────────────┐
 *        v                 v                 v
 *   ┌─────────┐      ┌──────────┐      ┌──────────┐
 *   │ 服务 A  │      │ 服务 B   │      │ 服务 C   │     ... (遍历所有服务)
 *   └────┬────┘      └────┬─────┘      └────┬─────┘
 *        │                │                 │
 *        v                v                 v
 *   ┌─────────────────────────────────────────────────┐
 *   │  对每个服务执行两步：                              │
 *   │                                                  │
 *   │  步骤1: 实例化服务实现，注册到本地缓存             │
 *   │         (装修店铺、把商品摆上架)                   │
 *   │                                                  │
 *   │  步骤2: 把服务信息注册到注册中心                   │
 *   │         (在黄页上登记"某某路某某号有这家店")        │
 *   └──────────────────────┬──────────────────────────┘
 *                          │
 *                          v
 *   ┌─────────────────────────────────────────────────┐
 *   │  启动网络服务器，开始监听请求                      │
 *   │  (正式开门迎客，等待消费者上门)                     │
 *   └─────────────────────────────────────────────────┘
 * </pre>
 *
 * @author HGL
 * @since 2025/9/5 15:41
 */
public class ProviderBootstrap {

    /**
     * 初始化服务提供者——完成开店的全部准备工作，然后开门迎客。
     *
     * <p>这个方法是 RPC 服务端启动的核心流程。它接收一个服务注册信息列表，
     * 对每个服务依次执行：实例化 -> 本地注册 -> 远程注册，最后启动服务器。</p>
     *
     * <p>为什么需要"本地注册"和"远程注册"两步？</p>
     * <ul>
     *   <li><b>本地注册（LocalRegistry）</b>：把服务实现类的实例缓存在内存中，
     *       当请求到来时，可以快速通过服务名找到对应的实现对象并执行方法。
     *       这就像店铺里的"货架"，商品摆在上面，顾客要买时随手就拿得到。</li>
     *   <li><b>远程注册（Registry）</b>：把服务的地址（host:port）和名称
     *       注册到注册中心（如 ZooKeeper、Etcd、Nacos），让消费者能发现我们。
     *       这就像在地图上标注店铺位置，让顾客能搜到你。</li>
     * </ul>
     *
     * @param serviceRegisterInfoList 服务注册信息列表，每个元素包含服务名和实现类
     */
    public static void init(List<ServiceRegisterInfo<?>> serviceRegisterInfoList) {
        // 读取全局配置——就像开店前先看看营业执照和经营规范
        final RpcConfig rpcConfig = RpcApplication.getRpcConfig();

        // 遍历所有要注册的服务，逐个"开店"
        for (ServiceRegisterInfo<?> serviceRegisterInfo : serviceRegisterInfoList) {
            String serviceName = serviceRegisterInfo.getServiceName();

            // ======== 步骤 1：装修店铺、摆上货架 ========
            // 通过反射创建服务实现类的实例，然后注册到本地缓存中
            // 这样当请求到来时，可以直接从缓存中取出实例执行方法，避免每次反射创建
            try {
                Object serviceInstance = serviceRegisterInfo.getImplClass().getDeclaredConstructor().newInstance();
                LocalRegistry.register(serviceName, serviceInstance);
            } catch (Exception e) {
                throw new RuntimeException(serviceName + " 服务实例化失败", e);
            }

            // ======== 步骤 2：在黄页上登记地址 ========
            // 把服务名和服务器地址注册到注册中心
            // 消费者就能通过注册中心发现我们："哦，UserService 在 192.168.1.100:8080"
            registryToCenter(rpcConfig, serviceName);
        }

        // ======== 步骤 3：开门迎客 ========
        // 启动网络服务器（基于 Vert.x），开始监听指定端口
        // 从这一刻起，消费者发来的请求就能被接收和处理了
        VertxServerFactory.getInstance(rpcConfig.getProtocol()).doStart(rpcConfig.getServerPort());
    }

    /**
     * 将服务注册到注册中心——在黄页上登记"我在这里，提供某某服务"。
     *
     * <p>注册到注册中心的信息包括：</p>
     * <ul>
     *   <li>服务名（serviceName）—— 我提供什么服务</li>
     *   <li>服务主机（serviceHost）—— 我在哪里</li>
     *   <li>服务端口（servicePort）—— 门牌号是多少</li>
     * </ul>
     *
     * <p>注册成功后，消费者就可以通过服务名查询到我们的地址了。
     * 当服务下线时，注册信息也会被自动清理（取决于注册中心的实现）。</p>
     *
     * @param rpcConfig   RPC 全局配置（包含注册中心类型、服务器地址等）
     * @param serviceName 要注册的服务名称
     * @throws RuntimeException 如果注册失败（如注册中心不可达）
     */
    private static void registryToCenter(RpcConfig rpcConfig, String serviceName) {
        // 获取注册中心实例（ZooKeeper / Etcd / Nacos 等）
        RegistryConfig registryConfig = rpcConfig.getRegistryConfig();
        Registry registry = RegistryFactory.getInstance(registryConfig.getRegistry());

        // 构建服务元信息——告诉注册中心"我是谁、我在哪"
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName(serviceName);
        serviceMetaInfo.setServiceHost(rpcConfig.getServerHost());
        serviceMetaInfo.setServicePort(rpcConfig.getServerPort());

        // 执行注册——把名片递交给注册中心
        try {
            registry.register(serviceMetaInfo);
        } catch (Exception e) {
            throw new RuntimeException(serviceName + " 服务注册失败", e);
        }
    }
}
