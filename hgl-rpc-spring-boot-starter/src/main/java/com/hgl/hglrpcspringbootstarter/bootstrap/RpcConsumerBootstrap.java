package com.hgl.hglrpcspringbootstarter.bootstrap;

import com.hgl.hglrpc.proxy.ServiceProxyFactory;
import com.hgl.hglrpcspringbootstarter.annotation.RpcReference;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

import java.lang.reflect.Field;

/**
 * RPC 消费者引导类 —— "自动注入远程服务的代理人"
 *
 * <p>实现 {@link BeanPostProcessor} 接口，在每个 Bean 初始化完成后扫描其字段，
 * 找到带有 {@code @RpcReference} 注解的字段，为其注入 RPC 代理对象。
 *
 * <p>工作流程：
 * <pre>
 *   Spring 创建 Bean → BeanPostProcessor.postProcessAfterInitialization()
 *       → 遍历 Bean 的所有字段
 *       → 找到带 @RpcReference 的字段
 *       → 通过 ServiceProxyFactory 创建代理对象
 *       → 反射将代理对象注入到字段中
 *
 *   之后调用该字段的方法时，实际走的是 RPC 远程调用。
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/9/5 16:27
 */
public class RpcConsumerBootstrap implements BeanPostProcessor {

    @Override
    public Object postProcessAfterInitialization(Object bean, @NonNull String beanName) throws BeansException {
        Class<?> beanClass = bean.getClass();

        // 遍历对象的所有字段
        Field[] declaredFields = beanClass.getDeclaredFields();
        for (Field field : declaredFields) {
            RpcReference rpcReference = field.getAnnotation(RpcReference.class);
            if (rpcReference != null) {
                // 确定服务接口类（默认取字段类型）
                Class<?> interfaceClass = rpcReference.interfaceClass();
                if (interfaceClass == void.class) {
                    interfaceClass = field.getType();
                }

                // 创建 RPC 代理对象
                Object proxyObject = ServiceProxyFactory.getProxy(interfaceClass);

                // 通过反射注入代理对象到字段
                try {
                    field.setAccessible(true);
                    field.set(bean, proxyObject);
                    field.setAccessible(false);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException("为字段注入代理对象失败", e);
                }
            }
        }
        return BeanPostProcessor.super.postProcessAfterInitialization(bean, beanName);
    }
}
