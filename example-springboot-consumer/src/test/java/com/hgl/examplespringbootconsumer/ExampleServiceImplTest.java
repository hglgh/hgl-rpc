package com.hgl.examplespringbootconsumer;

import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @ClassName: ExampleServiceImplTest
 * @Package: com.hgl.examplespringbootconsumer
 * @Description:
 * @Author HGL
 * @Create: 2025/9/5 16:41
 */
@SpringBootTest
class ExampleServiceImplTest {
    @Resource
    private ExampleServiceImpl exampleService;

    @Test
    void test() {
        exampleService.test();
    }
}