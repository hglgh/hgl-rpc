package com.hgl.example.common.model;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户实体类 —— RPC 通信中的"信封"
 *
 * <p>在整个 RPC 框架里，这个类扮演着"包裹"的角色：
 * 消费者把它交给框架，框架把它通过网络"快递"给提供者，提供者处理后再"寄回"。
 * 因此它必须实现 {@link Serializable}，让 Java 序列化器能把它转成字节流在网络上传输。</p>
 *
 * <h3>为什么需要 serialVersionUID？</h3>
 * <p>序列化和反序列化时，JVM 用这个 ID 来校验发送方和接收方的类版本是否一致。
 * 就像快递单号，两边必须对得上，否则会抛 {@code InvalidClassException}。</p>
 *
 * <h3>Lombok {@code @Data} 注解的作用</h3>
 * <p>一个注解顶一套模板代码，编译期自动生成：</p>
 * <ul>
 *   <li>{@code getName() / setName()} —— getter/setter</li>
 *   <li>{@code equals() / hashCode()} —— 对象比较和哈希</li>
 *   <li>{@code toString()} —— 打印友好输出</li>
 * </ul>
 * <p>这样我们只需要关注字段定义，不用写一堆样板代码。</p>
 *
 * <h3>使用示例</h3>
 * <pre>{@code
 *   User user = new User();
 *   user.setName("张三");
 *   // 经过 RPC 调用后，远端拿到的也是同样字段的 User 对象
 * }</pre>
 *
 * @author HGL
 * @see com.hgl.example.common.service.UserService —— 依赖此实体的服务接口
 */
@Data
public class User implements Serializable {

    /**
     * 序列化版本号 —— "快递单号"
     *
     * <p>保证发送端和接收端的 User 类版本一致。
     * 如果字段有增删改，记得同步修改此值，否则反序列化会失败。</p>
     */
    private static final long serialVersionUID = 1L;

    /**
     * 用户名 —— 用户的"名片"
     *
     * <p>在示例中，提供者会打印此字段来确认收到了正确的请求。</p>
     */
    private String name;
}
