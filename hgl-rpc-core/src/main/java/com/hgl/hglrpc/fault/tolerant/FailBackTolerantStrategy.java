package com.hgl.hglrpc.fault.tolerant;

import com.hgl.hglrpc.model.RpcRequest;
import com.hgl.hglrpc.model.RpcResponse;
import com.hgl.hglrpc.proxy.ServiceProxyFactory;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * @ClassName: FailBackTolerantStrategy
 * @Package: com.hgl.hglrpc.fault.tolerant
 * @Description: 降级到其他服务 - 容错策略
 * @Author HGL
 * @Create: 2025/9/5 14:34
 */
@Slf4j
public class FailBackTolerantStrategy implements TolerantStrategy{
    @Override
    public RpcResponse doTolerant(Map<String, Object> context, Exception e) {
        RpcRequest rpcRequest = (RpcRequest) context.getOrDefault("rpcRequest", null);
        if (rpcRequest == null){
            log.info("请求是空的，降不了级呀！");
            throw new RuntimeException(e.getMessage());
        }
        try {
            Class<?> serviceClass = Class.forName(rpcRequest.getServiceName( ));
            // 模拟一波接口调用
            Object mockProxy = ServiceProxyFactory.getMockProxy(serviceClass);
            Method method = mockProxy.getClass( ).getMethod(rpcRequest.getMethodName( ), rpcRequest.getParameterTypes( ));
            Object result = method.invoke(mockProxy, rpcRequest.getArgs( ));

            // 返回结果
            RpcResponse rpcResponse = new RpcResponse( );
            rpcResponse.setData(result);
            rpcResponse.setDataType(method.getReturnType( ));
            rpcResponse.setMessage("Fail Back Tolerant Strategy!");
            log.info("降级到其他服务 mock服务中或者返回404");
            return rpcResponse;
        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            log.info("类名/方法名/返回结果都找不到完犊子了");
            throw new RuntimeException(ex);
        }
    }

}
