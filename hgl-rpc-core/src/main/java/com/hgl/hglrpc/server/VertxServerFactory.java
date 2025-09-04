package com.hgl.hglrpc.server;

import com.hgl.hglrpc.spi.SpiLoader;

/**
 * @ClassName: VertxServerFactory
 * @Package: com.hgl.hglrpc.server
 * @Description:
 * @Author HGL
 * @Create: 2025/9/4 15:14
 */
public class VertxServerFactory {
    static {
        SpiLoader.load(VertxServer.class);
    }

    public static VertxServer getInstance(String key) {
        return SpiLoader.getInstance(VertxServer.class, key);
    }
}
