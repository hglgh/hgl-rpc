package com.hgl.hglrpc.config;

import com.hgl.hglrpc.fault.retry.RetryStrategyKeys;
import com.hgl.hglrpc.fault.tolerant.TolerantStrategyKeys;
import com.hgl.hglrpc.loadbalancer.LoadBalancerKeys;
import com.hgl.hglrpc.protocol.ProtocolKeys;
import com.hgl.hglrpc.serializer.SerializerKeys;
import lombok.Data;

/**
 * RPC 框架全局配置 —— "控制中心的总开关面板"
 *
 * <p>这个类集中管理了 RPC 框架的所有可配置项。
 * 框架启动时会从配置文件（application.properties 或 application.yml）中
 * 读取这些配置，如果配置文件中没有指定，就使用下面的默认值。
 *
 * <p>配置加载流程：
 * <pre>
 *   application.yml                     RpcConfig 对象
 *   ┌──────────────────┐               ┌──────────────────┐
 *   │ rpc:              │  ConfigUtils │                  │
 *   │   name: my-rpc    │ ──────────→  │ name = "my-rpc"  │
 *   │   serializer: kryo│  .loadConfig │ serializer = "kryo"│
 *   │   ...             │              │ ...              │
 *   └──────────────────┘               └──────────────────┘
 * </pre>
 *
 * <p>配置项速查表：
 * <pre>
 *   配置项             默认值         说明
 *   ──────────────     ─────────     ──────────────────────────────
 *   name               hgl-rpc       框架名称
 *   version            1.0           框架版本
 *   serverHost         localhost     服务端主机
 *   serverPort         8080          服务端端口
 *   mock               false         是否启用模拟调用
 *   serializer         jdk           序列化方式
 *   protocol           tcp           传输协议（tcp/http）
 *   loadBalancer       round_robin   负载均衡策略
 *   retryStrategy      no            重试策略
 *   tolerantStrategy   fail_fast     容错策略
 *   requestTimeout     10000         请求超时时间（毫秒）
 *   registryConfig     -             注册中心配置（见 RegistryConfig）
 *   circuitBreakerEnabled false      是否启用熔断器
 *   circuitBreakerFailureThreshold 5  熔断器失败阈值
 *   circuitBreakerOpenTimeoutMs  30000  熔断器打开超时（毫秒）
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/9/1 10:33
 */
@Data
public class RpcConfig {

    /** 框架名称 —— 给框架起个名字，方便日志标识 */
    private String name = "hgl-rpc";

    /** 框架版本号 */
    private String version = "1.0";

    /** 服务端监听的主机地址 */
    private String serverHost = "localhost";

    /** 服务端监听的端口号 */
    private Integer serverPort = 8080;

    /** 是否启用模拟调用（调试时使用，不真正发起远程调用） */
    private boolean mock = false;

    /** 序列化器名称（jdk/json/kryo/hessian），通过 SPI 加载对应实现 */
    private String serializer = SerializerKeys.JDK;

    /** 角色：provider（服务提供者）或 consumer（服务消费者） */
    private String role = "provider";

    /** 传输协议（tcp/http），决定使用哪种 Server 和 Client */
    private String protocol = ProtocolKeys.TCP;

    /** 负载均衡策略（round_robin/random/consistent_hash） */
    private String loadBalancer = LoadBalancerKeys.ROUND_ROBIN;

    /** 重试策略（no/fixed_interval/random_interval） */
    private String retryStrategy = RetryStrategyKeys.NO;

    /** 容错策略（fail_fast/fail_over/fail_safe/fail_back） */
    private String tolerantStrategy = TolerantStrategyKeys.FAIL_FAST;

    /** 注册中心配置（嵌套对象） */
    private RegistryConfig registryConfig = new RegistryConfig();

    /** RPC 请求超时时间（毫秒），超时后抛出 TimeoutException */
    private Long requestTimeout = 10000L;

    /**
     * 是否启用熔断器 —— “电路保险丝的总开关”
     *
     * <p>默认关闭（false）。开启后，ServiceProxy 会在每次调用时经过熔断器保护：
     * <pre>
     *   调用流程：
     *   发送请求 → 熔断器检查 → 重试 → 容错
     *                    │
     *         ┌──── CLOSED：放行
     *         ├── OPEN：直接拒绝（快速失败，保护客户端资源）
     *         └── HALF_OPEN：试探性放行一次
     * </pre>
     *
     * <p>适用场景：当下游服务已明确不可用时，避免客户端持续发起无意义的调用，
     * 给服务端恢复时间，防止级联故障扩散。
     *
     * @see com.hgl.hglrpc.fault.circuitbreaker.DefaultCircuitBreaker
     */
    private boolean circuitBreakerEnabled = false;

    /**
     * 熔断器失败阈值 —— “连续失败多少次后触发熔断”
     *
     * <p>默认值 5。当连续失败次数达到此阈值时，熔断器从 CLOSED 转为 OPEN。
     * 建议根据业务场景调整：
     * <ul>
     *   <li>核心服务（支付、订单）：3-5 次（快速熔断，避免雪崩）</li>
     *   <li>非核心服务（日志、埋点）：10+ 次（容忍更多抖动）</li>
     * </ul>
     */
    private int circuitBreakerFailureThreshold = 5;

    /**
     * 熔断器打开超时时间（毫秒） —— “熔断多久后尝试恢复”
     *
     * <p>默认值 30000（30秒）。熔断器处于 OPEN 状态超过此时间后，
     * 自动转为 HALF_OPEN 状态，允许一次试探性调用：
     * <ul>
     *   <li>试探成功：熔断器恢复为 CLOSED，正常放行</li>
     *   <li>试探失败：重新进入 OPEN，再等 30 秒</li>
     * </ul>
     */
    private long circuitBreakerOpenTimeoutMs = 30000L;
}
