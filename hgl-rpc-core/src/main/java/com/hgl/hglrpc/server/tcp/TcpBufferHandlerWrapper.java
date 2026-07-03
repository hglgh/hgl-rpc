package com.hgl.hglrpc.server.tcp;

import com.hgl.hglrpc.protocol.ProtocolConstant;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.parsetools.RecordParser;

/**
 * TCP 粘包拆包处理器 —— "把混在一起的包裹一个个分开"
 *
 * <p>TcpBufferHandlerWrapper 使用装饰者模式，基于 Vert.x 的 {@link RecordParser}
 * 对原始的 Buffer 处理能力进行增强，解决 TCP 粘包/拆包问题。
 *
 * <p>什么是粘包/拆包？
 * <pre>
 *   TCP 是<b>字节流</b>协议，没有消息边界的概念。
 *   就像一条传送带上连续不断地传送包裹，但传送带不会在每个包裹之间停顿。
 *
 *   ╔═══════════════════════════════════════════════════════════════════╗
 *   ║  发送端连续发出 3 个消息（信封A、信封B、信封C）：                  ║
 *   ║                                                                   ║
 *   ║  发送：  [═══信封A═══] [═══信封B═══] [═══信封C═══]               ║
 *   ║                                                                   ║
 *   ║  接收端可能收到的几种情况：                                       ║
 *   ║                                                                   ║
 *   ║  ① 正常：  [═══信封A═══] [═══信封B═══] [═══信封C═══]             ║
 *   ║  ② 粘包：  [═══信封A═══][信封B一半] [信封B另一半═══信封C═══]     ║
 *   ║  ③ 拆包：  [信封A前半]  [信封A后半] [═══信封B═══] [═══信封C═══]  ║
 *   ║                                                                   ║
 *   ║  如果不处理，你可能把信封A的后半截和信封B的前半截当成一个消息！    ║
 *   ╚═══════════════════════════════════════════════════════════════════╝
 * </pre>
 *
 * <p>解决思路：定长头 + 变长体（两阶段读取）
 * <pre>
 *   ┌─────────────────────────────────────────────────────────────────┐
 *   │                两阶段读取状态机                                  │
 *   │                                                                 │
 *   │  阶段1：固定读取 17 字节（消息头）                               │
 *   │    → 从头部的 bodyLength 字段得知消息体的长度                    │
 *   │                                                                 │
 *   │  阶段2：再读取 bodyLength 字节（消息体）                         │
 *   │    → 此时拿到了一个完整的消息（头 + 体）                        │
 *   │    → 交给 bufferHandler 处理                                    │
 *   │    → 重置状态，回到阶段1，准备读下一条消息                      │
 *   │                                                                 │
 *   │         读 17 字节          读 bodyLength 字节                   │
 *   │        ┌──────────┐       ┌──────────────┐                     │
 *   │   ────▶│  阶段1   │──────▶│    阶段2     │──── 回到阶段1        │
 *   │        │（读消息头）│       │ （读消息体）  │                     │
 *   │        └──────────┘       └──────────────┘                     │
 *   │                                                                 │
 *   │  这就像：先看信封上的"内含X页"标记，再精确读取X页内容。          │
 *   └─────────────────────────────────────────────────────────────────┘
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/9/4 11:00
 * @see com.hgl.hglrpc.protocol.ProtocolConstant 协议常量，定义了消息头长度（17字节）
 * @see RecordParser Vert.x 的记录解析器，支持定长/分隔符模式
 */
public class TcpBufferHandlerWrapper implements Handler<Buffer> {

    /**
     * RecordParser —— "智能分拣机"
     *
     * <p>Vert.x 提供的内置解析器，可以在两种模式间动态切换：
     * <ul>
     *   <li>{@code fixedSizeMode(n)}：精确读取 n 个字节后触发回调</li>
     *   <li>{@code delimitedMode(delimiter)}：读到分隔符后触发回调</li>
     * </ul>
     * 我们只使用 fixedSizeMode，在消息头和消息体之间切换读取长度。
     */
    private final RecordParser recordParser;

    /**
     * 构造函数 —— "初始化分拣机"
     *
     * <p>传入最终的消息处理器 bufferHandler，
     * 当 RecordParser 拼出一条完整消息后，会调用这个 handler 进行处理。
     *
     * @param bufferHandler 完整消息的处理器（如协议解码器）
     */
    public TcpBufferHandlerWrapper(Handler<Buffer> bufferHandler) {
        recordParser = initRecordParser(bufferHandler);
    }

    /**
     * 处理到达的数据 —— "把传送带上的数据喂给分拣机"
     *
     * <p>每次 TCP socket 收到数据时调用此方法。
     * 数据可能不完整（只来了消息头的一部分），也可能包含多条消息，
     * RecordParser 会负责缓存和切分。
     *
     * @param buffer 到达的数据块
     */
    @Override
    public void handle(Buffer buffer) {
        recordParser.handle(buffer);
    }

    /**
     * 初始化 RecordParser —— "配置分拣机的工作规则"
     *
     * <p>创建一个两阶段状态机：
     * <ol>
     *   <li>第一阶段：固定读取 {@link ProtocolConstant#MESSAGE_HEADER_LENGTH}（17）字节</li>
     *   <li>从头部 offset=13 处读取 bodyLength（4 字节 int）</li>
     *   <li>第二阶段：切换为读取 bodyLength 字节</li>
     *   <li>拼接完整消息，交给 bufferHandler</li>
     *   <li>重置状态，回到第一阶段</li>
     * </ol>
     *
     * <p>消息头部的字节布局（共 17 字节）：
     * <pre>
     *   offset:  0    1    2    3    4    5............12   13...........16
     *         ┌────┬────┬────┬────┬────┬───────────────┬───────────────┐
     *         │mag │ver │ser │typ │sta │  requestId    │  bodyLength   │
     *         │1B  │1B  │1B  │1B  │1B  │  8 bytes      │  4 bytes      │
     *         └────┴────┴────┴────┴────┴───────────────┴───────────────┘
     *                                                ↑
     *                                          buffer.getInt(13)
     *                                          从这里读取 body 长度
     * </pre>
     *
     * @param bufferHandler 完整消息的处理器
     * @return 配置好的 RecordParser
     */
    private RecordParser initRecordParser(Handler<Buffer> bufferHandler) {
        // 创建 parser，初始模式：固定读取 17 字节（消息头长度）
        RecordParser parser = RecordParser.newFixed(ProtocolConstant.MESSAGE_HEADER_LENGTH);

        parser.setOutput(new Handler<Buffer>() {
            /**
             * 当前阶段要读取的消息体长度
             * -1 表示"正在读消息头"（阶段1），>=0 表示"正在读消息体"（阶段2）
             */
            int size = -1;

            /**
             * 拼接缓冲区 —— 用于组装一条完整消息
             *
             * <p>先在阶段1存入头部，再在阶段2追加体部，
             * 拼完后交给 bufferHandler 处理，然后重置。
             */
            Buffer resultBuffer = Buffer.buffer();

            @Override
            public void handle(Buffer buffer) {
                if (-1 == size) {
                    // ====== 阶段1：刚读完 17 字节的消息头 ======

                    // 从头部 offset=13 读取 bodyLength —— "看看信封上写了几页"
                    size = buffer.getInt(13);

                    // 切换到阶段2：接下来读取 bodyLength 个字节
                    parser.fixedSizeMode(size);

                    // 把头部存入拼接缓冲区
                    resultBuffer.appendBuffer(buffer);
                } else {
                    // ====== 阶段2：刚读完 bodyLength 字节的消息体 ======

                    // 把消息体追加到头部后面 —— 拼成一条完整消息
                    resultBuffer.appendBuffer(buffer);

                    // 完整消息已就绪！交给 bufferHandler 处理（协议解码、业务逻辑等）
                    bufferHandler.handle(resultBuffer);

                    // ====== 重置状态，准备读取下一条消息 ======
                    parser.fixedSizeMode(ProtocolConstant.MESSAGE_HEADER_LENGTH);
                    size = -1;
                    resultBuffer = Buffer.buffer();
                }
            }
        });
        return parser;
    }
}
