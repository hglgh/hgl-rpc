package com.hgl.examplespringbootprovider;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * <h2>Spring Boot 提供者应用的上下文加载测试</h2>
 * <p>
 * 与消费者的冒烟测试同理——这是一次"通电检查"：
 * 确认提供者应用的 Spring 容器能正常启动，
 * 所有 Bean（包括 RPC 服务暴露相关的 Bean）装配正确。
 *
 * <p><b>前置条件：</b>需要 etcd 注册中心可用，因为
 * 提供者启动时会向注册中心注册自身服务。
 */
@SpringBootTest
class ExampleSpringbootProviderApplicationTests {

    /**
     * <h3>测试目标：Spring 容器能否正常启动</h3>
     * <h3>期望行为</h3>
     * 上下文加载成功，不抛异常。这意味着提供者的
     * 服务注册、端口监听等自动配置都已就绪。
     */
    @Test
    void contextLoads() {
    }

}
