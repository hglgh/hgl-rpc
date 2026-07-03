package com.hgl.hglrpcspringbootstarter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot Starter 应用入口 —— "自动装配的起点"
 *
 * <p>这是一个标准的 Spring Boot 启动类。在实际使用中，
 * 用户在自己的项目中引入 hgl-rpc-spring-boot-starter 依赖，
 * 然后在启动类上加 {@code @EnableRpc} 注解即可启用 RPC 功能。
 *
 * @author HGL
 */
@SpringBootApplication
public class HglRpcSpringBootStarterApplication {

    public static void main(String[] args) {
        SpringApplication.run(HglRpcSpringBootStarterApplication.class, args);
    }
}
