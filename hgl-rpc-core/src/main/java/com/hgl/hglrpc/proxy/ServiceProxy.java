package com.hgl.hglrpc.proxy;

import cn.hutool.core.collection.CollUtil;
import com.hgl.hglrpc.RpcApplication;
import com.hgl.hglrpc.config.RpcConfig;
import com.hgl.hglrpc.constant.RpcConstant;
import com.hgl.hglrpc.fault.circuitbreaker.CircuitBreaker;
import com.hgl.hglrpc.fault.circuitbreaker.DefaultCircuitBreaker;
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
import java.util.concurrent.CompletableFuture;

/**
 * 远程调用的代理人（ServiceProxy）
 *
 * <p>想象这样一个场景：你（消费者）想寄一封信给远方的朋友（服务提供者），
 * 但你不知道朋友的确切地址，也不在乎信件走哪条路，更不想操心万一寄丢了怎么办。
 * 你只需要把信交给代理人，代理人会帮你——查地址、选快递、挂失重寄——一条龙搞定。</p>
 *
 * <p>这个类就是 RPC 框架里的"代理人"。它基于 JDK 动态代理机制，
 * 让消费者调用远程服务就像调用本地方法一样简单。
 * 消费者完全感知不到网络通信、服务发现、负载均衡、重试容错等细节。</p>
 *
 * <h3>一次远程调用的完整旅程：</h3>
 * <pre>
 *  ┌──────────┐      ┌─────────────┐      ┌──────────────┐       ┌──────────────┐
 *  │ 业务代码   │  ──> │  构造请求    │ ──>  │ 注册中心发现    │ ──>   │  负载均衡     │
 *  │ 调用接口   │      │ (打包快递)   │      │ (查地址簿)     │       │ (挑快递员)    │
 *  └──────────┘      └─────────────┘      └──────────────┘       └──────┬───────┘
 *                                                                       │
 *                                                                       v
 *  ┌──────────┐      ┌─────────────┐      ┌──────────────┐      ┌──────────────┐
 *  │ 返回结果   │ <──  │  解析响应    │  <── │ 容错处理       │ <──  │  发送请求     │
 *  │ (收件回执) │      │ (拆开回信)   │      │ (挂失重寄)     │      │ (寄出快递)    │
 *  └──────────┘      └─────────────┘      └──────────────┘      └──────────────┘
 * </pre>
 *
 * <h3>熔断保护机制：</h3>
 * <pre>
 *  ┌──────────┐       ┌─────────────┐       ┌──────────────┐
 *  │ 发送请求   │ ──>   │ 熔断器检查    │  ──>  │ CLOSED：放行  │
 *  │          │       │ (电路保险丝)  │       │ OPEN：拒绝    │
 *  │          │       │             │       │ HALF_OPEN：试探│
 *  └──────────┘       └─────────────┘       └──────────────┘
 * </pre>
 *
 * <p>同时支持同步（{@link #invoke}）和异步（{@link #invokeAsync}）两种调用模式。
 *
 * <p>这个类是 RPC 框架中的"代理人"，负责处理远程方法调用。</p>
 * <p>InvocationHandler 是 JDK 动态代理机制中的一个接口，它定义了代理对象必须实现的 invoke 方法，该方法在代理对象上调用任何方法时都会被调用。</p>
 *
 * @author HGL
 * @since 2025/8/29 17:08
 */
@Slf4j
public class ServiceProxy implements InvocationHandler {

    /**
     * 全局熔断器实例 —— “整个客户端的电路保险丝”
     *
     * <p>当某个服务节点连续失败达到阈值时，熔断器自动打开，
     * 后续请求快速失败，避免对已不可用的服务持续发起无意义的调用。
     * 当配置的 circuitBreakerEnabled=false 时，熔断器不会被创建，相关逻辑会被跳过。
     */
    private volatile CircuitBreaker circuitBreaker;

    /**
     * 代理调用的核心入口——所有远程方法调用都从这里经过。
     *
     * <p>就像代理人收到你的信件后，依次执行以下步骤：</p>
     * <ol>
     *   <li>把你的口信整理成标准格式（构造 RPC 请求）</li>
     *   <li>去地址簿查一下，看看谁家提供这种服务（注册中心服务发现）</li>
     *   <li>在众多候选地址中挑一个最合适的（负载均衡）</li>
     *   <li>把信寄出去，如果丢了就重试，实在不行就走容错（发送请求 + 重试 + 容错）</li>
     *   <li>收到回信，把结果交还给你（返回响应数据）</li>
     * </ol>
     *
     * @param proxy  代理对象本身（通常不需要直接使用）
     * @param method 被调用的方法（比如 UserService.getUserById）
     * @param args   方法参数（比如 [1L]）
     * @return 远程调用的返回结果
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // ======================== 第一步：打包快递 ========================
        // 把方法名、参数等信息封装成标准的 RPC 请求报文，
        // 就像把口信写成格式规范的信件
        RpcRequest rpcRequest = buildRpcRequest(method, args);

        // ======================== 第二步：查地址簿 ========================
        // 去注册中心（相当于电话黄页）查询哪些机器提供这个服务
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        List<ServiceMetaInfo> serviceMetaInfoList = getServiceMetaInfoList(rpcRequest, rpcConfig);

        // ======================== 第三步：挑快递员 ========================
        // 多个服务节点都有能力处理请求，用负载均衡算法挑一个最合适的
        ServiceMetaInfo selectedServiceMetaInfo = loadBalanceSelect(rpcRequest, rpcConfig, serviceMetaInfoList);

        // ======================== 第四步：寄信并等待回音 ========================
        // 发送请求，如果失败了会自动重试；重试也没用，就启动容错策略
        RpcResponse rpcResponse = sendRequestWithRetry(rpcRequest, rpcConfig, selectedServiceMetaInfo, serviceMetaInfoList);

        // ======================== 第五步：拆开回信 ========================
        // 从响应报文中取出实际的数据，交还给调用方
        return rpcResponse.getData();
    }

    /**
     * 异步调用远程方法 —— "寄出快递后拿到取件码，不必原地等"
     *
     * <p>与 {@link #invoke} 相同的流程，但以异步方式返回结果。
     * 适合对延迟不敏感、或需要并发发起多个调用的场景。</p>
     *
     * <p>典型用法：
     * <pre>
     *   CompletableFuture&lt;Object&gt; future = serviceProxy.invokeAsync(proxy, method, args);
     *   future.thenAccept(result -&gt; {
     *       // 异步处理结果，不阻塞当前线程
     *       System.out.println("远程调用结果：" + result);
     *   });
     * </pre>
     *
     * <p>流程：构造请求 → 服务发现 → 负载均衡 → 异步发送请求 → CompletableFuture 返回
     *
     * @param proxy  代理对象
     * @param method 被调用的方法
     * @param args   方法参数
     * @return 包含调用结果的 CompletableFuture（调用方可通过 thenAccept/thenApply 等回调链式处理）
     */
    public CompletableFuture<Object> invokeAsync(Object proxy, Method method, Object[] args) {
        // 第一步：构造 RPC 请求报文
        RpcRequest rpcRequest = buildRpcRequest(method, args);

        // 第二步：从注册中心发现可用服务节点
        RpcConfig rpcConfig = RpcApplication.getRpcConfig();
        List<ServiceMetaInfo> serviceMetaInfoList = getServiceMetaInfoList(rpcRequest, rpcConfig);

        // 第三步：负载均衡选择目标节点
        ServiceMetaInfo selectedServiceMetaInfo = loadBalanceSelect(rpcRequest, rpcConfig, serviceMetaInfoList);

        // 第四步：异步发送请求，返回 CompletableFuture
        // thenApply 在响应到达后自动提取 data 字段，调用方可继续链式处理
        return VertxClientFactory.getInstance(rpcConfig.getProtocol())
                .doRequestAsync(rpcRequest, selectedServiceMetaInfo)
                .thenApply(RpcResponse::getData);
    }

    /**
     * 构造 RPC 请求报文——把方法调用信息打包成标准化的请求。
     *
     * <p>为什么需要这一步？因为远程调用不比本地调用，调用的是一个 JVM 里的方法，
     * 而网络上传输的是字节流。我们必须把"调哪个服务、调哪个方法、传什么参数"
     * 这些信息序列化成一个请求对象，对端收到后才能正确还原并执行。</p>
     *
     * @param method 被调用的方法对象
     * @param args   方法的实际参数
     * @return 封装好的 RPC 请求对象
     */
    private RpcRequest buildRpcRequest(Method method, Object[] args) {
        // 通过方法声明类获取服务名（如 com.hgl.example.service.UserService）
        String serviceName = method.getDeclaringClass().getName();
        return RpcRequest.builder()
                .serviceName(serviceName)               // 服务全限定名——告诉对端"我要调谁"
                .methodName(method.getName())            // 方法名——告诉对端"调哪个方法"
                .parameterTypes(method.getParameterTypes()) // 参数类型——防止同名方法歧义
                .args(args)                              // 实际参数——方法调用的真实入参
                .build();
    }

    /**
     * 从注册中心发现可用的服务提供者列表——就像翻电话黄页查地址。
     *
     * <p>为什么需要服务发现？在微服务架构中，服务提供者的实例可能有多个
     * （多机部署、动态扩缩容），地址不是固定的。
     * 通过注册中心，消费者可以实时获取当前可用的服务实例列表，
     * 无需硬编码地址，实现服务的动态发现和自动故障转移。</p>
     *
     * @param rpcRequest RPC 请求（用于提取服务名）
     * @param rpcConfig  RPC 全局配置（用于获取注册中心类型）
     * @return 可用服务节点的元信息列表
     * @throws RuntimeException 如果没有找到任何可用的服务地址
     */
    private List<ServiceMetaInfo> getServiceMetaInfoList(RpcRequest rpcRequest, RpcConfig rpcConfig) {
        // 根据配置的注册中心类型（如 ZooKeeper、Etcd、Nacos 等）获取注册中心实例
        Registry registry = RegistryFactory.getInstance(rpcConfig.getRegistryConfig().getRegistry());

        // 构建服务查询条件：用服务名 + 默认版本号来匹配
        ServiceMetaInfo serviceMetaInfo = new ServiceMetaInfo();
        serviceMetaInfo.setServiceName(rpcRequest.getServiceName());
        serviceMetaInfo.setServiceVersion(RpcConstant.DEFAULT_SERVICE_VERSION);

        // 向注册中心发起服务发现——"谁在提供 UserService 服务？"
        List<ServiceMetaInfo> serviceMetaInfoList = registry.serviceDiscovery(serviceMetaInfo.getServiceKey());

        // 如果一个可用地址都没找到，那这封信就寄不出去了
        if (CollUtil.isEmpty(serviceMetaInfoList)) {
            throw new RuntimeException("暂无服务地址");
        }
        return serviceMetaInfoList;
    }

    /**
     * 负载均衡——在多个服务节点中挑一个来处理本次请求。
     *
     * <p>为什么需要负载均衡？假如有 3 台机器都提供 UserService，
     * 如果每次都只找第一台，那第一台很快就会被压垮，其他两台却闲着。
     * 负载均衡就像一个聪明的调度员，按照策略（轮询、随机、一致性哈希等）
     * 把请求合理地分配给不同节点，让每台机器都不至于太忙或太闲。</p>
     *
     * @param rpcRequest          RPC 请求
     * @param rpcConfig           RPC 配置（用于获取负载均衡策略类型）
     * @param serviceMetaInfoList 所有可用的服务节点列表
     * @return 负载均衡选中的那一个服务节点
     */
    private ServiceMetaInfo loadBalanceSelect(RpcRequest rpcRequest, RpcConfig rpcConfig, List<ServiceMetaInfo> serviceMetaInfoList) {
        // 根据配置获取负载均衡器实例（如轮询、随机、一致性哈希等）
        LoadBalancer loadBalancer = LoadBalancerFactory.getInstance(rpcConfig.getLoadBalancer());

        // 把调用的方法名传给负载均衡器——某些策略（如一致性哈希）需要这个信息
        // 来保证同一个方法的请求尽量路由到同一个节点，提升缓存命中率
        Map<String, Object> requestParams = new HashMap<>();
        requestParams.put("methodName", rpcRequest.getMethodName());

        return loadBalancer.select(requestParams, serviceMetaInfoList);
    }

    /**
     * 发送 RPC 请求并支持自动重试——寄信、等回音，丢了就再寄一封。
     *
     * <p>为什么要有重试机制？网络是不可靠的，一次请求可能因为网络抖动、
     * 服务端临时过载等原因失败。对于读请求（幂等操作），自动重试可以
     * 有效提升调用成功率，用户甚至感知不到中间发生过失败。
     * 但如果重试了 N 次都不行，说明问题比较严重，就交给容错策略去处理了。</p>
     *
     * <p>重试与容错的关系：</p>
     * <pre>
     *   发送请求  ──失败  ──>  重试1 ── 失败 ──>重试2 ── 失败──>容错处理
     *      │                  │              │              │
     *      v                  v              v              v
     *   成功就返回          成功就返回      成功就返回     降级/切换节点/抛异常
     * </pre>
     *
     * <p>如果配置了熔断器（circuitBreakerEnabled=true），会在重试之前先检查熔断状态：
     * <pre>
     *   发送请求 → 熔断器检查 → CLOSED：放行重试 → 成功/失败
     *                         → OPEN：直接拒绝（快速失败）
     *                         → HALF_OPEN：试探性放行一次
     * </pre>
     *
     * @param rpcRequest               RPC 请求
     * @param rpcConfig                RPC 配置
     * @param selectedServiceMetaInfo  当前选中的服务节点
     * @param serviceMetaInfoList      所有可用的服务节点（容错时可能需要换一个）
     * @return RPC 响应
     */
    private RpcResponse sendRequestWithRetry(RpcRequest rpcRequest, RpcConfig rpcConfig, ServiceMetaInfo selectedServiceMetaInfo, List<ServiceMetaInfo> serviceMetaInfoList) {
        try {
            // 如果启用了熔断器，先经过熔断器保护（避免对已不可用的服务持续发起无意义的调用）
            if (rpcConfig.isCircuitBreakerEnabled()) {
                return executeWithCircuitBreaker(rpcRequest, rpcConfig, selectedServiceMetaInfo);
            }
            // 未启用熔断器，直接执行带重试的调用
            return doRetry(rpcRequest, rpcConfig, selectedServiceMetaInfo);
        } catch (Exception e) {
            // 重试全部失败了，启动容错机制——换个方案继续尝试
            return handleTolerance(rpcRequest, rpcConfig, selectedServiceMetaInfo, serviceMetaInfoList, e);
        }
    }

    /**
     * 通过熔断器执行带重试的调用 —— "经过电路保险丝的 RPC 调用"
     *
     * <p>使用双重检查锁（DCL）懒初始化熔断器实例：
     * 首次调用时才根据 RpcConfig 配置创建熔断器，之后直接复用。</p>
     *
     * <p>熔断器内部包裹了 {@link #doRetry} 调用，即：
     * <pre>
     *   熔断器.execute(() -&gt; 重试策略.doRetry(() -&gt; 实际发送请求))
     * </pre>
     * 如果熔断器处于 OPEN 状态，直接抛出异常，不会执行内部的重试和发送逻辑。</p>
     *
     * @param rpcRequest              RPC 请求
     * @param rpcConfig               RPC 配置（包含熔断器参数）
     * @param selectedServiceMetaInfo 当前选中的服务节点
     * @return RPC 响应
     * @throws Exception 熔断器打开时抛出 RuntimeException，或内部重试失败时抛出原始异常
     */
    private RpcResponse executeWithCircuitBreaker(RpcRequest rpcRequest, RpcConfig rpcConfig, ServiceMetaInfo selectedServiceMetaInfo) throws Exception {
        // DCL 懒初始化熔断器（volatile + synchronized + 双重 null 检查）
        if (circuitBreaker == null) {
            synchronized (this) {
                if (circuitBreaker == null) {
                    // 根据配置创建熔断器：失败阈值、打开超时、半开恢复需连续成功 2 次
                    circuitBreaker = new DefaultCircuitBreaker(
                            rpcConfig.getCircuitBreakerFailureThreshold(),
                            rpcConfig.getCircuitBreakerOpenTimeoutMs(),
                            2
                    );
                }
            }
        }
        // 将带重试的调用作为整体交给熔断器包裹
        return circuitBreaker.execute(() -> doRetry(rpcRequest, rpcConfig, selectedServiceMetaInfo));
    }

    /**
     * 执行带重试的发送请求 —— "寄信，丢了就再寄"
     *
     * <p>从 SPI 获取配置的重试策略实例，将实际发送请求的动作包装成
     * {@link java.util.concurrent.Callable} 交给重试策略执行。</p>
     *
     * <p>重试策略内部会决定：
     * <ul>
     *   <li>是否重试（NoRetryStrategy 不重试，FixedIntervalRetryStrategy 重试 3 次）</li>
     *   <li>重试间隔（固定间隔 / 指数退避等）</li>
     *   <li>何时放弃（达到最大次数后抛出异常）</li>
     * </ul>
     *
     * @param rpcRequest              RPC 请求
     * @param rpcConfig               RPC 配置（用于获取重试策略和网络协议类型）
     * @param selectedServiceMetaInfo 目标服务节点
     * @return RPC 响应
     * @throws Exception 所有重试都失败后抛出异常
     */
    private RpcResponse doRetry(RpcRequest rpcRequest, RpcConfig rpcConfig, ServiceMetaInfo selectedServiceMetaInfo) throws Exception {
        // 从 SPI 获取重试策略实例（如 NoRetryStrategy、FixedIntervalRetryStrategy）
        RetryStrategy retryStrategy = RetryStrategyFactory.getInstance(rpcConfig.getRetryStrategy());

        // 将发送请求的动作包装成 Callable，交给重试策略执行
        return retryStrategy.doRetry(() -> {
            try {
                // 通过网络客户端（TCP/HTTP）把请求发出去
                return VertxClientFactory.getInstance(rpcConfig.getProtocol()).doRequest(rpcRequest, selectedServiceMetaInfo);
            } catch (Throwable e) {
                throw new RuntimeException("发送请求失败：" + e.getMessage());
            }
        });
    }

    /**
     * 容错处理——当前节点不行了，换个方案继续。
     *
     * <p>当重试多次仍然失败时，说明当前服务节点可能已经不可用了。
     * 这时候不是简单地抛异常就完事了——我们有备选方案：</p>
     * <ul>
     *   <li><b>故障转移（Failover）</b>：换一个节点重新试</li>
     *   <li><b>故障安全（FailSafe）</b>：吞掉异常，返回空结果</li>
     *   <li><b>快速失败（FailFast）</b>：直接抛出异常，让调用方知道出问题了</li>
     *   <li><b>其他策略</b>：如 FailBack、Forking 等</li>
     * </ul>
     *
     * <p>为什么需要容错？因为分布式系统中，故障是常态而不是意外。
     * 一个好的 RPC 框架应该尽可能保证业务可用，而不是一遇到问题就全线崩溃。</p>
     *
     * @param rpcRequest               RPC 请求
     * @param rpcConfig                RPC 配置
     * @param selectedServiceMetaInfo  出故障的那个节点
     * @param serviceMetaInfoList      所有可用节点（供故障转移使用）
     * @param e                        原始异常信息
     * @return 容错后的 RPC 响应
     */
    private RpcResponse handleTolerance(RpcRequest rpcRequest, RpcConfig rpcConfig, ServiceMetaInfo selectedServiceMetaInfo, List<ServiceMetaInfo> serviceMetaInfoList, Exception e) {
        // 获取配置的容错策略实例
        TolerantStrategy tolerantStrategy = TolerantStrategyFactory.getInstance(rpcConfig.getTolerantStrategy());

        // 把所有可能用到的信息打包传给容错策略——它自己决定怎么处理
        // 比如故障转移策略会从 serviceMetaInfoList 里换一个节点重试
        Map<String, Object> requestTolerantParamMap = new HashMap<>();
        requestTolerantParamMap.put("rpcRequest", rpcRequest);
        requestTolerantParamMap.put("selectedServiceMetaInfo", selectedServiceMetaInfo);
        requestTolerantParamMap.put("serviceMetaInfoList", serviceMetaInfoList);
        return tolerantStrategy.doTolerant(requestTolerantParamMap, e);
    }
}
