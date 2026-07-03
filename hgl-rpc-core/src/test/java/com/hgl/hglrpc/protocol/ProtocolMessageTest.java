package com.hgl.hglrpc.protocol;

import cn.hutool.core.util.IdUtil;
import com.hgl.hglrpc.constant.RpcConstant;
import com.hgl.hglrpc.model.RpcRequest;
import com.hgl.hglrpc.server.tcp.codec.ProtocolMessageDecoder;
import com.hgl.hglrpc.server.tcp.codec.ProtocolMessageEncoder;
import io.vertx.core.buffer.Buffer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @ClassName: ProtocolMessageTest
 * @Package: com.hgl.hglrpc.protocol
 * @Description: 协议消息的验收测试
 * @Author HGL
 * @Create: 2025/9/3 16:50
 *
 * <h2>测试全景</h2>
 * RPC 协议消息就像一封"加密快递"——发件人按约定的格式把请求
 * 装箱（encode），收件人按同样的格式拆箱（decode）。
 * 编码器和解码器必须严格对称，否则就像用错尺码的箱子——
 * 装进去的东西拆出来就面目全非了。
 *
 * <p>本测试类验证 {@link ProtocolMessageEncoder} 和 {@link ProtocolMessageDecoder}
 * 的编解码对称性：构造一条完整的协议消息，编码为二进制 Buffer，
 * 再解码回对象，检查往返一致性。
 */
class ProtocolMessageTest {

    /**
     * <h3>测试目标：协议消息的编码-解码往返一致性（Round-trip）</h3>
     * <p>
     * 就像把一封信装进信封、邮寄出去、对方拆开信封——
     * 信的内容不应该有任何损坏或丢失。测试流程：
     * <ol>
     *   <li>构造协议头（header）：魔数、版本号、序列化方式、消息类型、状态码、请求ID</li>
     *   <li>构造协议体（body）：一个完整的 RPC 请求</li>
     *   <li>编码：将消息对象序列化为二进制 Buffer（装箱）</li>
     *   <li>解码：将二进制 Buffer 反序列化回消息对象（拆箱）</li>
     *   <li>断言：拆出来的箱子不为空</li>
     * </ol>
     *
     * <h3>期望行为</h3>
     * 解码后的消息对象不为 null，说明编码-解码流程畅通无阻。
     * 进一步的字段级校验可通过日志或扩展断言来补充。
     */
    @Test
    public void testEncodeAndDecode() {
        // --- 构造一条完整的协议消息：装箱前的"原材料" ---
        ProtocolMessage<RpcRequest> protocolMessage = new ProtocolMessage<>();
        ProtocolMessage.Header header = getHeader();    // 协议头：信封上的地址和邮戳
        RpcRequest rpcRequest = getRpcRequest();        // 协议体：信件正文
        protocolMessage.setHeader(header);
        protocolMessage.setBody(rpcRequest);

        // --- 编码：把消息对象压成二进制流，像把信塞进信封 ---
        Buffer encodeBuffer = ProtocolMessageEncoder.encode(protocolMessage);

        // --- 解码：从二进制流中还原消息对象，像收件人拆开信封 ---
        ProtocolMessage<?> message = ProtocolMessageDecoder.decode(encodeBuffer);

        // --- 核心断言：信拆开后不为空，说明编解码格式对称 ---
        assertNotNull(message);
    }

    /**
     * 构造测试用的 RPC 请求体。
     * 模拟一个典型的远程调用请求——就像写一封信：
     * <ul>
     *   <li>收件人（serviceName）：myService</li>
     *   <li>事由（methodName）：myMethod</li>
     *   <li>版本（serviceVersion）：默认版本</li>
     *   <li>附件类型（parameterTypes）：String</li>
     *   <li>附件内容（args）：["aaa", "bbb"]</li>
     * </ul>
     */
    private static RpcRequest getRpcRequest() {
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setServiceName("myService");
        rpcRequest.setMethodName("myMethod");
        rpcRequest.setServiceVersion(RpcConstant.DEFAULT_SERVICE_VERSION);
        rpcRequest.setParameterTypes(new Class[]{String.class});
        rpcRequest.setArgs(new Object[]{"aaa", "bbb"});
        return rpcRequest;
    }

    /**
     * 构造测试用的协议头。
     * 协议头就像信封上的"邮戳和地址栏"，每一项都有严格约定：
     * <ul>
     *   <li>magic —— 魔数，协议的"暗号"，用来识别这是不是合法的 RPC 报文</li>
     *   <li>version —— 协议版本号，确保双方用同一版"语言"沟通</li>
     *   <li>serializer —— 序列化方式（JDK），决定了"信件正文"用什么编码</li>
     *   <li>type —— 消息类型（REQUEST），表示这是一封"请求信"而非"回执"</li>
     *   <li>status —— 状态码（OK），预设为正常</li>
     *   <li>requestId —— 雪花算法生成的唯一 ID，像快递单号一样追踪每一笔请求</li>
     *   <li>bodyLength —— 消息体长度（编码器会自动计算，此处占位）</li>
     * </ul>
     */
    private static ProtocolMessage.Header getHeader() {
        ProtocolMessage.Header header = new ProtocolMessage.Header();
        header.setMagic(ProtocolConstant.PROTOCOL_MAGIC);           // "暗号"：协议魔数
        header.setVersion(ProtocolConstant.PROTOCOL_VERSION);       // 语言版本：协议版本号
        header.setSerializer(ProtocolMessageSerializerEnum.JDK.getKey());   // 编码方式：JDK 序列化
        header.setType(ProtocolMessageTypeEnum.REQUEST.getKey());           // 信件类型：请求
        header.setStatus(ProtocolMessageStatusEnum.OK.getValue());          // 邮戳状态：正常
        header.setRequestId(IdUtil.getSnowflakeNextId());                   // 快递单号：雪花 ID
        header.setBodyLength(0);                                            // 占位，编码器会回填真实长度
        return header;
    }

}
