package com.hgl.hglrpc.protocol;

import cn.hutool.core.util.IdUtil;
import com.hgl.hglrpc.constant.RpcConstant;
import com.hgl.hglrpc.model.RpcRequest;
import com.hgl.hglrpc.server.tcp.codec.ProtocolMessageDecoder;
import com.hgl.hglrpc.server.tcp.codec.ProtocolMessageEncoder;
import io.vertx.core.buffer.Buffer;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @ClassName: ProtocolMessageTest
 * @Package: com.hgl.hglrpc.protocol
 * @Description:
 * @Author HGL
 * @Create: 2025/9/3 16:50
 */
class ProtocolMessageTest {
    @Test
    public void testEncodeAndDecode() throws IOException {
        // 构造消息
        ProtocolMessage<RpcRequest> protocolMessage = new ProtocolMessage<>();
        ProtocolMessage.Header header = getHeader();
        RpcRequest rpcRequest = getRpcRequest();
        protocolMessage.setHeader(header);
        protocolMessage.setBody(rpcRequest);

        Buffer encodeBuffer = ProtocolMessageEncoder.encode(protocolMessage);
        ProtocolMessage<?> message = ProtocolMessageDecoder.decode(encodeBuffer);
        assertNotNull(message);
    }

    private static RpcRequest getRpcRequest() {
        RpcRequest rpcRequest = new RpcRequest();
        rpcRequest.setServiceName("myService");
        rpcRequest.setMethodName("myMethod");
        rpcRequest.setServiceVersion(RpcConstant.DEFAULT_SERVICE_VERSION);
        rpcRequest.setParameterTypes(new Class[]{String.class});
        rpcRequest.setArgs(new Object[]{"aaa", "bbb"});
        return rpcRequest;
    }

    private static ProtocolMessage.Header getHeader() {
        ProtocolMessage.Header header = new ProtocolMessage.Header();
        header.setMagic(ProtocolConstant.PROTOCOL_MAGIC);
        header.setVersion(ProtocolConstant.PROTOCOL_VERSION);
        header.setSerializer((byte) ProtocolMessageSerializerEnum.JDK.getKey());
        header.setType((byte) ProtocolMessageTypeEnum.REQUEST.getKey());
        header.setStatus((byte) ProtocolMessageStatusEnum.OK.getValue());
        header.setRequestId(IdUtil.getSnowflakeNextId());
        header.setBodyLength(0);
        return header;
    }

}