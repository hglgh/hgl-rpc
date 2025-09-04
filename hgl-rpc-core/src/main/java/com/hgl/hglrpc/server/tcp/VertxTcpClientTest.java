package com.hgl.hglrpc.server.tcp;

import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;

/**
 * @ClassName: VertxTcpClient
 * @Package: com.hgl.hglrpc.server.tcp
 * @Description:
 * @Author HGL
 * @Create: 2025/9/3 15:12
 */
public class VertxTcpClientTest {

    public void start(int port) {
        // 创建 Vert.x 实例
        Vertx vertx = Vertx.vertx();
        vertx.createNetClient().connect(port, "localhost", result -> {
            if (result.succeeded()) {
                System.out.println("Connected to TCP server");
                // 获取连接
                NetSocket socket = result.result();
                for (int i = 0; i < 1000; i++) {
                    // 发送数据
                    Buffer buffer = Buffer.buffer();
                    String str = "Hello, server!Hello, server!Hello, server!Hello, server!";
                    buffer.appendInt(0);
                    buffer.appendInt(str.getBytes().length);
                    buffer.appendBytes(str.getBytes());
                    socket.write(buffer);
                }
                // 接收响应
                socket.handler(buffer -> System.out.println("Received response from server: " + buffer.toString()));
            } else {
                // 处理错误
                System.err.println("Failed to connect to TCP server");
            }
        });
    }

    public static void main(String[] args) {
        new VertxTcpClientTest().start(8080);
    }
}
