package com.hgl.hglrpc.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @ClassName: ServiceRegisterInfo
 * @Package: com.hgl.hglrpc.model
 * @Description: 服务注册信息类
 * @Author HGL
 * @Create: 2025/9/5 15:43
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceRegisterInfo<T> {
    /**
     * 服务名称
     */
    private String serviceName;

    /**
     * 实现类
     */
    private Class<? extends T> implClass;
}
