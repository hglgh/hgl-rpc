package com.hgl.examplespringbootconsumer;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @ClassName: ExampleServiceImplTest
 * @Package: com.hgl.examplespringbootconsumer
 * @Description: Spring Boot 消费者端 RPC 服务调用测试
 * @Author HGL
 * @Create: 2025/9/5 16:41
 *
 * <h2>测试全景</h2>
 * 这是一个端到端的"实战演练"——在 Spring Boot 消费者应用中，
 * 通过注入的 {@link ExampleServiceImpl} 发起一次真实的 RPC 远程调用，
 * 验证从"拨号"到"通话"的整条链路是否畅通：
 * <ol>
 *   <li>消费者端的代理对象正确注入</li>
 *   <li>请求通过网络发送到提供者</li>
 *   <li>提供者正确执行并返回结果</li>
 * </ol>
 *
 * <p><b>前置条件：</b>需要消费者和提供者应用都已启动，且 etcd 注册中心可达。
 * 这就像打电话测试——电话线两头都得有人接。
 */
@SpringBootTest
class ExampleServiceImplTest {

    /** 通过 Spring 注入的服务实现——已由 RPC 代理包装 */
    @Resource
    private ExampleServiceImpl exampleService;

    /**
     * <h3>测试目标：端到端的 RPC 远程调用</h3>
     * <p>
     * 调用 {@code exampleService.test()} 方法，触发一次完整的 RPC 调用。
     * 就像拿起电话拨号——如果对方接听并正常应答，说明整个通信链路是通的。
     *
     * <h3>期望行为</h3>
     * 方法调用成功完成，不抛出异常。
     * 如果服务端未启动、网络不通或注册中心异常，此测试会直接失败。
     */
    @Test
    void test() {
        exampleService.test();
    }
}
