package com.hgl.hglrpc.serializer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hgl.hglrpc.model.RpcRequest;
import com.hgl.hglrpc.model.RpcResponse;

/**
 * JSON 序列化器 —— "JSON 格式的翻译官"
 *
 * <p>使用 Jackson（{@link ObjectMapper}）进行 JSON 序列化/反序列化。
 * JSON 是人类可读的文本格式，便于调试和跨语言交互。
 *
 * <p>核心难点：JSON 反序列化时会丢失 Java 类型信息。
 * <pre>
 *   例如：RpcRequest.args = [1L, "hello"]
 *   JSON 反序列化后：args = [1, "hello"]  ← 1L 变成了 Integer！
 *
 *   原因：JSON 没有 long/double 的概念，数字就是数字。
 *   解决：handleResponse/handleRequest 中强制"二次反序列化"——
 *         先按目标类型序列化回来，再反序列化为目标类型。
 * </pre>
 *
 * @Author HGL
 * @Create: 2025/9/1 16:37
 */
public class JsonSerializer implements Serializer {

    /** Jackson 的核心序列化器（线程安全，可共享） */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Override
    public <T> byte[] serialize(T obj) {
        try {
            return OBJECT_MAPPER.writeValueAsBytes(obj);
        } catch (Exception e) {
            throw new SerializeException("JSON 序列化失败", e);
        }
    }

    @Override
    public <T> T deserialize(byte[] bytes, Class<T> classType) {
        try {
            T obj = OBJECT_MAPPER.readValue(bytes, classType);
            // 处理 RPC 请求：参数类型可能丢失，需要二次反序列化
            if (obj instanceof RpcRequest) {
                return handleRequest((RpcRequest) obj, classType);
            }
            // 处理 RPC 响应：data 可能是 LinkedTreeMap 而非目标类型
            if (obj instanceof RpcResponse) {
                return handleResponse((RpcResponse) obj, classType);
            }
            return obj;
        } catch (Exception e) {
            throw new SerializeException("JSON 反序列化失败", e);
        }
    }

    /**
     * 修复 RpcRequest 中参数类型丢失的问题
     *
     * <p>JSON 反序列化 args 时，Jackson 只知道参数是 Object 类型，
     * 会将数字转为 Integer/Double，字符串转为 String。
     * 我们利用 parameterTypes 中声明的真实类型进行二次反序列化。
     */
    private <T> T handleRequest(RpcRequest rpcRequest, Class<T> type) {
        try {
            Class<?>[] parameterTypes = rpcRequest.getParameterTypes();
            Object[] args = rpcRequest.getArgs();
            for (int i = 0; i < parameterTypes.length; i++) {
                Class<?> clazz = parameterTypes[i];
                // 如果类型不匹配（如 Integer → Long），进行二次转换
                if (!clazz.isAssignableFrom(args[i].getClass())) {
                    byte[] argBytes = OBJECT_MAPPER.writeValueAsBytes(args[i]);
                    args[i] = OBJECT_MAPPER.readValue(argBytes, clazz);
                }
            }
            return type.cast(rpcRequest);
        } catch (Exception e) {
            throw new SerializeException("JSON 请求参数反序列化失败", e);
        }
    }

    /**
     * 修复 RpcResponse 中 data 类型丢失的问题
     *
     * <p>JSON 反序列化 data 时，Jackson 会将其解析为通用类型（LinkedTreeMap），
     * 而非具体的业务类型（如 User）。利用 dataType 字段进行二次反序列化。
     */
    private <T> T handleResponse(RpcResponse rpcResponse, Class<T> type) {
        try {
            byte[] dataBytes = OBJECT_MAPPER.writeValueAsBytes(rpcResponse.getData());
            rpcResponse.setData(OBJECT_MAPPER.readValue(dataBytes, rpcResponse.getDataType()));
            return type.cast(rpcResponse);
        } catch (Exception e) {
            throw new SerializeException("JSON 响应数据反序列化失败", e);
        }
    }
}
