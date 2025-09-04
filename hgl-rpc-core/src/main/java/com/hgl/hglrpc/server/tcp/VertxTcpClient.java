package com.hgl.hglrpc.server.tcp;

import io.vertx.core.Vertx;
import io.vertx.core.net.NetSocket;

/**
 * @ClassName: VertxTcpClient
 * @Package: com.hgl.hglrpc.server.tcp
 * @Description:
 * @Author HGL
 * @Create: 2025/9/3 15:12
 */
public class VertxTcpClient {

    public void start(int port) {
        // 创建 Vert.x 实例
        Vertx vertx = Vertx.vertx();
        vertx.createNetClient().connect(port, "localhost", result -> {
            if (result.succeeded()) {
                System.out.println("Connected to TCP server");
                // 获取连接
                NetSocket socket = result.result();
                // 发送数据
                socket.write("Hello, server!");
                // 接收响应
                socket.handler(buffer -> System.out.println("Received response from server: " + buffer.toString()));
            } else {
                // 处理错误
                System.err.println("Failed to connect to TCP server");
            }
        });
    }

    public static void main(String[] args) {
        new VertxTcpClient().start(8080);
    }
}
