package com.hgl.hglrpc.fault.tolerant;

import com.hgl.hglrpc.model.RpcRequest;
import com.hgl.hglrpc.model.RpcResponse;
import com.hgl.hglrpc.proxy.ServiceProxyFactory;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * 失败回调（降级）容错策略 —— 「正路走不通，试试备选方案」
 *
 * <p>当远程 RPC 调用失败时，不是直接报错，而是尝试降级到本地的 Mock 服务。
 * 就像你叫外卖，餐厅做不了这道菜，系统自动给你推荐了一个类似的替代菜品。</p>
 *
 * <p><b>核心理念：Fail Back（回退/降级）</b></p>
 * <p>这个策略的关键在于「降级」——不是放弃，而是退而求其次。
 * 通过 {@link ServiceProxyFactory#getMockProxy} 获取到 Mock 代理，
 * 调用本地的 Mock 实现来返回一个「说得过去」的结果。</p>
 *
 * <pre>
 * 调用流程：
 *
 *   RPC 调用失败
 *        │
 *        ▼
 *   ┌──────────────┐
 *   │  FailBack     │
 *   │  失败回调策略  │
 *   └──────┬───────┘
 *          │
 *          ▼
 *   ┌─────────────────────────────┐
 *   │  检查 rpcRequest 是否为空    │
 *   └──────────┬──────────────────┘
 *              │
 *       ┌──────┴──────┐
 *       ▼              ▼
 *    [为空]         [不为空]
 *       │              │
 *   抛出异常      ┌────┴─────────────────────┐
 *                 │ 1. 加载服务接口的 Class    │
 *                 │ 2. 获取 Mock 代理对象      │
 *                 │ 3. 反射调用 Mock 方法      │
 *                 │ 4. 包装返回结果            │
 *                 └────┬─────────────────────┘
 *                      │
 *                ┌─────┴─────┐
 *                ▼           ▼
 *            [调用成功]   [调用失败]
 *                │           │
 *          返回降级结果   抛出异常
 *          （Mock数据）  （真的没救了）
 * </pre>
 *
 * <p><b>适用场景：</b></p>
 * <ul>
 *   <li>服务降级：高峰期核心服务扛不住，非核心功能降级</li>
 *   <li>兜底策略：返回缓存数据、默认值或 Mock 数据</li>
 *   <li>用户体验保障：宁可返回「假数据」也不显示错误页面</li>
 * </ul>
 *
 * @author HGL
 * @since 2025/9/5
 */
@Slf4j
public class FailBackTolerantStrategy implements TolerantStrategy {

    /**
     * 执行失败回调/降级处理。
     *
     * <p>通过反射机制调用本地 Mock 代理的方法来获取降级结果。
     * 整个过程就像：快递送不了 → 查看包裹上有没有备选地址 → 有的话送到备选地址。</p>
     *
     * @param context 上下文信息，需要包含 "rpcRequest" 以便知道该降级到哪个服务
     * @param e       原始异常
     * @return RpcResponse 降级后的响应结果（来自 Mock 服务）
     * @throws RuntimeException 当请求为空或降级调用也失败时抛出
     */
    @Override
    public RpcResponse doTolerant(Map<String, Object> context, Exception e) {
        // ========== 第一步：取出原始请求 ==========
        RpcRequest rpcRequest = (RpcRequest) context.getOrDefault("rpcRequest", null);

        // 连请求都没有，那就真的降不了级了——巧妇难为无米之炊
        if (rpcRequest == null) {
            log.info("请求是空的，降不了级呀！");
            throw new RuntimeException(e.getMessage());
        }

        try {
            // ========== 第二步：加载服务接口的 Class 对象 ==========
            // 比如请求的是 UserService，这里就加载 UserService.class
            Class<?> serviceClass = Class.forName(rpcRequest.getServiceName());

            // ========== 第三步：获取 Mock 代理对象 ==========
            // Mock 代理是预先定义好的降级实现，比如查不到用户就返回一个默认用户
            Object mockProxy = ServiceProxyFactory.getMockProxy(serviceClass);

            // ========== 第四步：通过反射调用 Mock 代理的对应方法 ==========
            // 找到和原始请求同名同参数的方法，然后调用它
            Method method = mockProxy.getClass().getMethod(rpcRequest.getMethodName(), rpcRequest.getParameterTypes());
            Object result = method.invoke(mockProxy, rpcRequest.getArgs());

            // ========== 第五步：包装降级结果返回 ==========
            RpcResponse rpcResponse = new RpcResponse();
            rpcResponse.setData(result);
            rpcResponse.setDataType(method.getReturnType());
            rpcResponse.setMessage("Fail Back Tolerant Strategy!");
            log.info("降级到其他服务 mock服务中或者返回404");
            return rpcResponse;

        } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            // 降级也失败了，说明真的没救了：类找不到、方法找不到、或者调用出错
            log.info("类名/方法名/返回结果都找不到完犊子了");
            throw new RuntimeException(ex);
        }
    }
}
