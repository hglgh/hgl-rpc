package com.hgl.hglrpc.server.tcp;

import cn.hutool.core.util.IdUtil;
import com.hgl.hglrpc.RpcApplication;
import com.hgl.hglrpc.model.RpcRequest;
import com.hgl.hglrpc.model.RpcResponse;
import com.hgl.hglrpc.model.ServiceMetaInfo;
import com.hgl.hglrpc.protocol.ProtocolConstant;
import com.hgl.hglrpc.protocol.ProtocolMessage;
import com.hgl.hglrpc.protocol.ProtocolMessageSerializerEnum;
import com.hgl.hglrpc.protocol.ProtocolMessageTypeEnum;
import com.hgl.hglrpc.server.client.VertxClient;
import com.hgl.hglrpc.server.tcp.codec.ProtocolMessageDecoder;
import com.hgl.hglrpc.server.tcp.codec.ProtocolMessageEncoder;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetClient;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * TCP 客户端 —— "专属快递员，有自己的快递车和常用路线"
 *
 * <p>VertxTcpClient 是 {@link VertxClient} 接口的 TCP 实现，
 * 负责向远程 TCP 服务器发送 RPC 请求并等待响应。
 *
 * <p>核心设计模式：<b>单例 Vertx + 连接池 + 超时控制</b>
 *
 * <pre>
 *   ┌─────────────────────────────────────────────────────────────────────────┐
 *   │                      VertxTcpClient 的架构                              │
 *   │                                                                         │
 *   │   ┌─────────────────────────────────────────────────────────────────┐   │
 *   │   │  单例 Vertx 实例（整个 JVM 只有一个事件循环线程池）              │   │
 *   │   │      │                                                          │   │
 *   │   │      ├── NetClient（TCP 客户端，负责建立连接）                    │   │
 *   │   │      │                                                          │   │
 *   │   │      └── 连接池 CONNECTION_POOL                                 │   │
 *   │   │          ┌──────────────────────────────────────────────────┐   │   │
 *   │   │          │ "192.168.1.1:8080" ──▶ NetSocket A              │   │   │
 *   │   │          │ "192.168.1.2:8081" ──▶ NetSocket B              │   │   │
 *   │   │          │ "192.168.1.3:8082" ──▶ NetSocket C              │   │   │
 *   │   │          └──────────────────────────────────────────────────┘   │   │
 *   │   └─────────────────────────────────────────────────────────────────┘   │
 *   └─────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * <p>为什么 Vertx 和 NetClient 必须是单例？
 * <pre>
 *   ╔═══════════════════════════════════════════════════════════════════╗
 *   ║  Vertx.vertx() 每次调用都会创建：                                ║
 *   ║    - 一组事件循环线程（默认 = CPU 核心数 * 2）                    ║
 *   ║    - 一组工作线程池                                              ║
 *   ║    - Netty 的 EventLoopGroup                                     ║
 *   ║                                                                   ║
 *   ║  如果每次 RPC 调用都 Vertx.vertx()：                             ║
 *   ║    第1次请求 → 创建 16 个线程                                     ║
 *   ║    第2次请求 → 又创建 16 个线程                                   ║
 *   ║    第100次请求 → 1600 个线程在竞争 CPU！                          ║
 *   ║    结果：线程爆炸、GC 疯狂、延迟飙升                             ║
 *   ║                                                                   ║
 *   ║  单例模式：整个 JVM 只需要一套事件循环线程池，复用即可。          ║
 *   ╚═══════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <p>为什么需要连接池？
 * <ul>
 *   <li>TCP 三次握手需要 1.5 个 RTT，对于局域网约 0.1-1ms，跨机房可能 10-50ms</li>
 *   <li>复用已有连接，省去握手开销，直接发送数据</li>
 *   <li>使用 ConcurrentHashMap 存储 "host:port → NetSocket" 映射</li>
 *   <li>当连接关闭时，通过 closeHandler 自动从池中移除</li>
 *   <li>超时后也会移除可能已失效的连接</li>
 * </ul>
 *
 * @Author HGL
 * @Create: 2025/9/4 11:09
 * @see VertxClient 客户端接口
 * @see TcpBufferHandlerWrapper 粘包拆包处理器，用于接收完整响应
 * @see ProtocolMessageEncoder 协议编码器
 * @see ProtocolMessageDecoder 协议解码器
 */
@Slf4j
public class VertxTcpClient implements VertxClient {

    /**
     * Vertx 实例单例 —— "整个快递公司共享一套事件循环线程池"
     *
     * <p>static final 保证 JVM 生命周期内只创建一个 Vertx 实例。
     * 所有 TCP 连接的 I/O 事件都由这一组事件循环线程处理，
     * 避免了线程爆炸问题。
     */
    private static final Vertx VERTX = Vertx.vertx();

    /**
     * NetClient 单例 —— "共享一辆快递车"
     *
     * <p>所有出站 TCP 连接都通过这一个 NetClient 创建。
     * NetClient 内部会复用 Vertx 的事件循环，开销极小。
     */
    private static final NetClient NET_CLIENT = VERTX.createNetClient();

    /**
     * TCP 连接池 —— "常用路线的熟路快递员名单"
     *
     * <p>key = "host:port"（收件地址），value = NetSocket（已建立的 TCP 连接）。
     * 使用 ConcurrentHashMap 保证线程安全（可能多个线程同时发起 RPC 请求）。
     *
     * <p>连接的生命周期管理：
     * <pre>
     *   创建连接 ──▶ 放入池中 ──▶ 复用 ──▶ 关闭/超时 ──▶ 从池中移除
     *                                      │
     *                                      ├── closeHandler 自动移除
     *                                      └── TimeoutException 手动移除
     * </pre>
     */
    private static final ConcurrentHashMap<String, NetSocket> CONNECTION_POOL = new ConcurrentHashMap<>();

    /**
     * 发送 RPC 请求 —— "快递员出发送快递"
     *
     * <p>完整的请求流程：
     * <pre>
     *   1. 拼接地址 "host:port"
     *   2. 从连接池获取已有连接，或创建新连接（getOrCreateConnection）
     *   3. 编码 RPC 请求为二进制协议消息（sendRpcRequest）
     *   4. 写入 NetSocket 发送到服务端
     *   5. 注册响应处理器，通过 CompletableFuture 等待响应
     *   6. 带超时的阻塞等待，避免线程永久卡死
     * </pre>
     *
     * @param rpcRequest      请求参数，即要发送的"包裹内容"
     * @param serviceMetaInfo 服务元信息，包含目标地址（host + port）
     * @return RpcResponse 响应结果
     * @throws InterruptedException 线程中断异常
     * @throws ExecutionException   执行异常（网络错误、服务端报错等）
     */
    @Override
    @SuppressWarnings("unchecked")
    public RpcResponse doRequest(RpcRequest rpcRequest, ServiceMetaInfo serviceMetaInfo) throws InterruptedException, ExecutionException {
        // 拼接收件地址 —— "XX市XX区XX路XX号"
        String address = serviceMetaInfo.getServiceHost() + ":" + serviceMetaInfo.getServicePort();
        log.info("tcp client send request to {}", address);

        // 创建 CompletableFuture 用于异步等待响应 —— 快递员的"回执等待单"
        CompletableFuture<RpcResponse> responseFuture = new CompletableFuture<>();

        // 从连接池获取或创建连接，然后发送请求
        // thenAccept 表示"连接就绪后，立即发送 RPC 请求"
        getOrCreateConnection(address, serviceMetaInfo, responseFuture, rpcRequest)
                .thenAccept(socket -> {
                    // 连接就绪后发送请求 —— 快递员到达目的地，开始送件
                    sendRpcRequest(socket, rpcRequest, responseFuture);
                });

        // 带超时的等待响应 —— 设定"最迟几点回来"的 deadline
        long timeout = RpcApplication.getRpcConfig().getRequestTimeout();
        try {
            // 阻塞等待 CompletableFuture 完成，最多等 timeout 毫秒
            return responseFuture.get(timeout, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            // 超时了！移除可能已失效的连接，避免下次还用坏连接
            CONNECTION_POOL.remove(address);
            throw new RuntimeException("RPC 请求超时（" + timeout + "ms）", e);
        }
    }

    /**
     * 从连接池获取已有连接，或创建新连接 —— "找熟路的快递员，或派一个新快递员"
     *
     * <p>优先从池中取已有的 NetSocket（省去 TCP 三次握手开销），
     * 如果池中没有（第一次连接或之前的连接已断开），则创建新连接并放入池中。
     *
     * <p>连接创建后还会注册 closeHandler：
     * 当连接意外断开时（如服务端重启、网络中断），自动从池中移除，
     * 避免下次请求用到一个"已经下班"的快递员。
     *
     * @param address         "host:port" 地址字符串
     * @param serviceMetaInfo 服务元信息
     * @param responseFuture  响应的 CompletableFuture（连接创建失败时会 completeExceptionally）
     * @param rpcRequest      请求参数（当前未使用，保留用于未来扩展）
     * @return 包含 NetSocket 的 CompletableFuture
     */
    private CompletableFuture<NetSocket> getOrCreateConnection(String address, ServiceMetaInfo serviceMetaInfo,
                                                                CompletableFuture<RpcResponse> responseFuture,
                                                                RpcRequest rpcRequest) {
        CompletableFuture<NetSocket> socketFuture = new CompletableFuture<>();

        // 先从池中取 —— "看看有没有熟路的快递员可用"
        NetSocket cachedSocket = CONNECTION_POOL.get(address);
        if (cachedSocket != null) {
            socketFuture.complete(cachedSocket);
            return socketFuture;
        }

        // 池中没有，创建新连接 —— "派一个新快递员出发"
        NET_CLIENT.connect(serviceMetaInfo.getServicePort(), serviceMetaInfo.getServiceHost(), result -> {
            if (!result.succeeded()) {
                // 连接失败 —— 快递员出门就摔了一跤，通知调用方
                responseFuture.completeExceptionally(
                        new RuntimeException("Failed to connect to TCP server: " + result.cause().getMessage()));
                return;
            }
            NetSocket socket = result.result();
            // 放入连接池 —— "登记到常用快递员名单"
            CONNECTION_POOL.put(address, socket);
            // 监听连接关闭事件 —— 如果快递员"离职"了，从名单上划掉
            socket.closeHandler(v -> {
                CONNECTION_POOL.remove(address);
                log.info("连接已关闭，从池中移除: {}", address);
            });
            socketFuture.complete(socket);
        });

        return socketFuture;
    }

    /**
     * 发送 RPC 请求 —— "装车、发货"
     *
     * <p>将 RpcRequest 封装成符合自定义二进制协议的 {@link ProtocolMessage}，
     * 编码后写入 NetSocket 发送到服务端，然后注册响应处理器等待回复。
     *
     * <p>协议消息的组装过程：
     * <pre>
     *   ┌──────────────────────────────────────────────────────┐
     *   │                 ProtocolMessage 信封                  │
     *   │                                                      │
     *   │  Header（快递单）：                                    │
     *   │    magic     = 0x1   （防伪标识）                     │
     *   │    version   = 0x1   （协议版本）                     │
     *   │    serializer = 配置值 （JDK/JSON/Kryo/Hessian）      │
     *   │    type      = REQUEST （这是请求，不是响应）         │
     *   │    requestId = 雪花ID （唯一的快递单号）              │
     *   │                                                      │
     *   │  Body（包裹内容）：                                    │
     *   │    RpcRequest（服务名、方法名、参数...）              │
     *   └──────────────────────────────────────────────────────┘
     * </pre>
     *
     * @param socket          已建立的 TCP 连接
     * @param rpcRequest      RPC 请求参数
     * @param responseFuture  用于接收响应的 CompletableFuture
     */
    private void sendRpcRequest(NetSocket socket, RpcRequest rpcRequest, CompletableFuture<RpcResponse> responseFuture) {
        // ========== 组装协议消息 ==========
        ProtocolMessage<RpcRequest> protocolMessage = new ProtocolMessage<>();
        ProtocolMessage.Header header = new ProtocolMessage.Header();

        // 填写"快递单"的各个字段
        header.setMagic(ProtocolConstant.PROTOCOL_MAGIC);         // 防伪标识
        header.setVersion(ProtocolConstant.PROTOCOL_VERSION);     // 协议版本
        header.setSerializer(Objects.requireNonNull(
                ProtocolMessageSerializerEnum.getEnumByValue(
                        RpcApplication.getRpcConfig().getSerializer())).getKey());  // 序列化方式
        header.setType(ProtocolMessageTypeEnum.REQUEST.getKey()); // 消息类型：请求
        header.setRequestId(IdUtil.getSnowflakeNextId());         // 雪花算法生成唯一单号
        protocolMessage.setHeader(header);
        protocolMessage.setBody(rpcRequest);

        // ========== 编码并发送 —— "装车发货" ==========
        Buffer encodeBuffer = ProtocolMessageEncoder.encode(protocolMessage);
        socket.write(encodeBuffer);

        // ========== 注册响应处理器 —— "等签收回执" ==========
        // 用 TcpBufferHandlerWrapper 包装，确保收到完整响应后才处理
        TcpBufferHandlerWrapper bufferHandlerWrapper = new TcpBufferHandlerWrapper(
                buffer -> {
                    // 收到完整响应，解码并完成 Future
                    ProtocolMessage<RpcResponse> rpcResponseProtocolMessage =
                            (ProtocolMessage<RpcResponse>) ProtocolMessageDecoder.decode(buffer);
                    responseFuture.complete(rpcResponseProtocolMessage.getBody());
                }
        );
        socket.handler(bufferHandlerWrapper);
    }
}
