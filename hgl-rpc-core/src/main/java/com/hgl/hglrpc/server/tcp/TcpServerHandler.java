package com.hgl.hglrpc.server.tcp;

import com.hgl.hglrpc.model.RpcRequest;
import com.hgl.hglrpc.model.RpcResponse;
import com.hgl.hglrpc.protocol.ProtocolMessage;
import com.hgl.hglrpc.protocol.ProtocolMessageTypeEnum;
import com.hgl.hglrpc.registry.LocalRegistry;
import com.hgl.hglrpc.server.tcp.codec.ProtocolMessageDecoder;
import com.hgl.hglrpc.server.tcp.codec.ProtocolMessageEncoder;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;

/**
 * TCP 快递员的派件流程 —— 处理每一个 TCP 连接上的 RPC 请求
 *
 * <p>TcpServerHandler 实现了 Vert.x 的 {@code Handler<NetSocket>} 接口，
 * 每当有新的客户端 TCP 连接建立时，Vert.x 会调用 handle() 方法，
 * 就像网点门口来了一个取件人，快递员开始为其服务。
 *
 * <p>一个完整的"派件流程"如下：
 * <pre>
 *   ┌────────────────────────────────────────────────────────────────────────┐
 *   │                    TCP 快递员的派件流程                                   │
 *   │                                                                        │
 *   │  客户端 ──── TCP 连接 ────▶ NetSocket                                    │
 *   │                               │                                        │
 *   │                               ▼                                        │
 *   │                   TcpBufferHandlerWrapper                              │
 *   │                   （粘包拆包：把混在一起的包裹一个个分开）                     │
 *   │                               │                                        │
 *   │                               ▼                                        │
 *   │                   ProtocolMessageDecoder.decode()                      │
 *   │                   （拆信封：从二进制数据中还原出 RpcRequest）                │
 *   │                               │                                        │
 *   │                               ▼                                        │
 *   │                   LocalRegistry.get(serviceName)                       │
 *   │                   （查快递柜：找到目标服务的实现类）                          │
 *   │                               │                                        │
 *   │                               ▼                                        │
 *   │                   method.invoke(serviceInstance, args)                 │
 *   │                   （送货上门：通过反射调用目标方法）                          │
 *   │                               │                                        │
 *   │                               ▼                                        │
 *   │                   ProtocolMessageEncoder.encode()                      │
 *   │                   （封装回信：把响应结果编码为二进制）                        │
 *   │                               │                                        │
 *   │                               ▼                                        │
 *   │                   netSocket.write(responseBuffer)                      │
 *   │                   （发回签收单：写回客户端）                                │
 *   └────────────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/9/3 17:01
 * @see TcpBufferHandlerWrapper 粘包拆包处理器
 * @see ProtocolMessageDecoder 协议解码器
 * @see ProtocolMessageEncoder 协议编码器
 * @see LocalRegistry 本地服务注册表（"快递柜"）
 */
@Slf4j
@SuppressWarnings("unchecked")
public class TcpServerHandler implements Handler<NetSocket> {

    /**
     * 处理一个新的 TCP 连接 —— "快递员接单"
     *
     * <p>当客户端建立 TCP 连接后，此方法被调用。
     * 它会为该连接注册一个 {@link TcpBufferHandlerWrapper}，
     * 确保粘包拆包后的每一个完整消息都能被正确处理。
     *
     * @param netSocket 与客户端的 TCP 连接，相当于"取件人的快递柜通道"
     */
    @Override
    public void handle(NetSocket netSocket) {
        // 包装一层粘包拆包处理器 —— 快递员先学习"如何正确拆包裹"
        // TCP 是流式协议，可能多个消息粘在一起，也可能一个消息被拆成多段
        // TcpBufferHandlerWrapper 保证每次回调给我们的都是一个完整的"信封"
        TcpBufferHandlerWrapper tcpBufferHandlerWrapper = new TcpBufferHandlerWrapper(buffer -> {

            // ========== 第1步：拆信封 —— 从二进制还原出 RPC 请求 ==========
            // 解码器会校验魔数、读取头部、反序列化消息体
            ProtocolMessage<RpcRequest> protocolMessage =
                    (ProtocolMessage<RpcRequest>) ProtocolMessageDecoder.decode(buffer);
            RpcRequest rpcRequest = protocolMessage.getBody();

            // ========== 第2步：处理请求 —— 查快递柜、送货上门 ==========
            RpcResponse rpcResponse = new RpcResponse();
            try {
                // 从本地注册表中查找服务实现类 —— 查快递柜，找到包裹的收件地址
                Object serviceInstance = LocalRegistry.get(rpcRequest.getServiceName());
                if (serviceInstance == null) {
                    throw new RuntimeException("未找到服务: " + rpcRequest.getServiceName());
                }

                // 通过反射调用目标方法 —— 送货上门！
                // 1. 根据方法名和参数类型获取 Method 对象
                // 2. 调用 invoke 执行方法，传入参数
                Method method = serviceInstance.getClass().getMethod(
                        rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
                Object result = method.invoke(serviceInstance, rpcRequest.getArgs());

                // 封装返回结果 —— 对方签收了，准备签收回执
                rpcResponse.setData(result);
                rpcResponse.setDataType(method.getReturnType());
                rpcResponse.setMessage("ok");
            } catch (Exception e) {
                // 调用出错 —— 包裹损坏或地址有误，记录异常信息
                log.error("Error: ", e);
                rpcResponse.setMessage(e.getMessage());
                rpcResponse.setException(e);
            }

            // ========== 第3步：封装回信 —— 把签收结果编码发回 ==========
            // 复用请求消息的头部，但将类型改为 RESPONSE
            ProtocolMessage.Header header = protocolMessage.getHeader();
            header.setType(ProtocolMessageTypeEnum.RESPONSE.getKey());
            ProtocolMessage<RpcResponse> responseProtocolMessage = new ProtocolMessage<>(header, rpcResponse);

            // 编码为二进制 —— 把签收回执装进信封
            Buffer encode = ProtocolMessageEncoder.encode(responseProtocolMessage);

            // 写回客户端 —— 把信封扔回快递柜
            netSocket.write(encode);
        });

        // 将粘包拆包处理器注册到连接上 —— 快递员开始工作
        // 从此刻起，所有到达这个连接的数据都会先经过 TcpBufferHandlerWrapper 处理
        netSocket.handler(tcpBufferHandlerWrapper);
    }
}
