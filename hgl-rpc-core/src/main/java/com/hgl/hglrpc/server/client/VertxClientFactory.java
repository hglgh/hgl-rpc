package com.hgl.hglrpc.server.client;

import com.hgl.hglrpc.spi.SpiLoader;

/**
 * @ClassName: VertxClientFactory
 * @Package: com.hgl.hglrpc.server
 * @Description:
 * @Author HGL
 * @Create: 2025/9/4 15:31
 */
public class VertxClientFactory {
    static {
        SpiLoader.load(VertxClient.class);
    }

    public static VertxClient getInstance(String key) {
        return SpiLoader.getInstance(VertxClient.class, key);
    }
}
