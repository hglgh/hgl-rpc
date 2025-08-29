package com.hgl.example.consumer.proxy;

import java.lang.reflect.Proxy;

/**
 * @ClassName: ServiceProxyFactory
 * @Package: com.hgl.example.consumer.proxy
 * @Description: 服务代理工厂（用于创建代理对象）
 * @Author HGL
 * @Create: 2025/8/29 17:12
 */
public class ServiceProxyFactory {
    /**
     * 根据服务类获取代理对象
     *
     * @param serviceClass 服务类
     * @param <T>          服务类泛型
     * @return 服务代理对象
     */
    @SuppressWarnings("unchecked")
    public static <T> T getProxy(Class<T> serviceClass) {
        return (T) Proxy.newProxyInstance(
                serviceClass.getClassLoader(),
                new Class[]{serviceClass},
                new ServiceProxy());
    }
}
