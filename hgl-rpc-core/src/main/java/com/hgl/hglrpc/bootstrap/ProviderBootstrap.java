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
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

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
 *   提供者的启动之旅（并发开店流程）：
 *
 *   ┌──────────────────────────────────────────────────────────┐
 *   │              ProviderBootstrap.init()                    │
 *   └──────────────────────┬───────────────────────────────────┘
 *                          │
 *                          v
 *   ┌──────────────────────────────────────────────────────────┐
 *   │  创建线程池（固定大小，守护线程）                              │
 *   │  线程数 = 配置值 registerThreadPoolSize                    │
 *   │         若 ≤ 0 则自动计算 min(服务数, CPU核数*2)             │
 *   │  线程名 = 配置前缀 registerThreadNamePrefix + 序号          │
 *   └──────────────────────┬───────────────────────────────────┘
 *                          │
 *        ┌─────────────────┼─────────────────┐
 *        v                 v                 v
 *   ┌─────────┐      ┌──────────┐      ┌──────────┐
 *   │ 服务 A   │      │ 服务 B    │      │ 服务 C   │  ... (并发执行)
 *   │ Thread-1│      │ Thread-2 │      │ Thread-3 │
 *   └────┬────┘      └────┬─────┘      └────┬─────┘
 *        │                │                 │
 *        v                v                 v
 *   ┌─────────────────────────────────────────────────┐
 *   │  每个服务在线程池中并发执行两步：                     │
 *   │                                                 │
 *   │  步骤1: 实例化服务实现，注册到本地缓存                 │
 *   │         (装修店铺、把商品摆上架)                     │
 *   │         LocalRegistry 基于 ConcurrentHashMap     │
 *   │         天然线程安全，可并发写入                     │
 *   │                                                 │
 *   │  步骤2: 把服务信息注册到注册中心                      │
 *   │         (在黄页上登记"某某路某某号有这家店")           │
 *   │         网络 I/O 操作，并发可显著减少总耗时           │
 *   └──────────────────────┬──────────────────────────┘
 *                          │
 *                          v
 *   ┌─────────────────────────────────────────────────┐
 *   │  CompletableFuture.allOf().join()               │
 *   │  等待所有服务注册完成（栅栏机制）                     │
 *   │  所有店铺都准备好后，才进入下一步                     │
 *   └──────────────────────┬──────────────────────────┘
 *                          │
 *                          v
 *   ┌─────────────────────────────────────────────────┐
 *   │  关闭线程池（executor.shutdown()）                 │
 *   │  释放资源，避免线程泄漏                             │
 *   └──────────────────────┬──────────────────────────┘
 *                          │
 *                          v
 *   ┌─────────────────────────────────────────────────┐
 *   │  启动网络服务器，开始监听请求                         │
 *   │  (正式开门迎客，等待消费者上门)                       │
 *   └─────────────────────────────────────────────────┘
 * </pre>
 *
 * @author HGL
 * @since 2025/9/5 15:41
 */
public class ProviderBootstrap {

    /**
     * 初始化服务提供者——并发完成开店的全部准备工作，然后开门迎客。
     *
     * <p>这个方法是 RPC 服务端启动的核心流程。它接收一个服务注册信息列表，
     * 利用线程池 + {@link CompletableFuture} 对所有服务<b>并发</b>执行：
     * 实例化 -&gt; 本地注册 -&gt; 远程注册，最后启动服务器。</p>
     *
     * <p>为什么需要"本地注册"和"远程注册"两步？</p>
     * <ul>
     *   <li><b>本地注册（LocalRegistry）</b>：把服务实现类的实例缓存在内存中，
     *       当请求到来时，可以快速通过服务名找到对应的实现对象并执行方法。
     *       这就像店铺里的"货架"，商品摆在上面，顾客要买时随手就拿得到。
     *       <br>底层使用 {@link java.util.concurrent.ConcurrentHashMap}，线程安全。</li>
     *   <li><b>远程注册（Registry）</b>：把服务的地址（host:port）和名称
     *       注册到注册中心（如 ZooKeeper、Etcd、Nacos），让消费者能发现我们。
     *       这就像在地图上标注店铺位置，让顾客能搜到你。
     *       <br>涉及网络 I/O，并发注册可显著减少启动总耗时。</li>
     * </ul>
     *
     * <p><b>并发策略：</b></p>
     * <ul>
     *   <li>线程池大小由 {@link RpcConfig#getRegisterThreadPoolSize()} 控制：
     *       配置值 &gt; 0 时使用配置值；否则自动计算 {@code min(服务数量, CPU核数 * 2)}</li>
     *   <li>使用守护线程（daemon），线程名由 {@link RpcConfig#getRegisterThreadNamePrefix()}
     *       配置前缀 + 序号组成，便于问题排查</li>
     *   <li>通过 {@code CompletableFuture.allOf().join()} 等待所有服务注册完毕后，
     *       才启动网络服务器，确保"所有店铺都准备好后才开门迎客"</li>
     * </ul>
     *
     * @param serviceRegisterInfoList 服务注册信息列表，每个元素包含服务名和实现类
     * @throws RuntimeException 如果任一服务实例化失败或注册中心注册失败
     */
    public static void init(List<ServiceRegisterInfo<?>> serviceRegisterInfoList) {
        // 读取全局配置——就像开店前先看看营业执照和经营规范
        final RpcConfig rpcConfig = RpcApplication.getRpcConfig();

        // 遍历所有要注册的服务，并发"开店"——利用线程池同时完成多个服务的本地注册 + 远程注册
        // 本地注册（LocalRegistry）基于 ConcurrentHashMap，天然线程安全；
        // 远程注册涉及网络 I/O，并发执行可以显著减少总耗时

        // 从配置中读取线程池大小：配置值 > 0 时使用配置值，否则自动计算
        // 自动计算公式：min(待注册服务数量, CPU核心数 * 2)，兼顾并发度与资源消耗
        int poolSize = rpcConfig.getRegisterThreadPoolSize() > 0
                ? rpcConfig.getRegisterThreadPoolSize()
                : Math.min(serviceRegisterInfoList.size(), Runtime.getRuntime().availableProcessors() * 2);
        // 从配置中读取线程名前缀，并配合自增序号为每个线程命名
        // 例如：provider-register-1, provider-register-2, ...
        // 这样在线程 dump 和日志中可以快速定位注册相关线程
        String threadNamePrefix = rpcConfig.getRegisterThreadNamePrefix();
        AtomicInteger threadIndex = new AtomicInteger(0);

        // 创建固定大小线程池，使用守护线程，线程名 = 配置前缀 + 序号
        ExecutorService executor = Executors.newFixedThreadPool(
                poolSize,
                r -> {
                    // 线程命名示例：provider-register-1, provider-register-2
                    Thread t = new Thread(r, threadNamePrefix + "-" + threadIndex.incrementAndGet());
                    t.setDaemon(true);
                    return t;
                }
        );

        try {
            // 为每个服务创建一个异步注册任务，直接转为数组，省去中间 List
            CompletableFuture<?>[] futures = serviceRegisterInfoList.stream()
                    .map(serviceRegisterInfo -> CompletableFuture.runAsync(() -> {
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
                    }, executor))
                    .toArray(CompletableFuture[]::new);

            // 等待所有服务的注册任务完成——所有店铺都准备好后才开门迎客
            CompletableFuture.allOf(futures).join();
        } finally {
            // 注册完成后关闭线程池，避免资源泄漏
            executor.shutdown();
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
