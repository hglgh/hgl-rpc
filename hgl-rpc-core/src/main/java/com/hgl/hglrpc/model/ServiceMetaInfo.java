package com.hgl.hglrpc.model;

import cn.hutool.core.util.StrUtil;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 服务元信息 —— "服务的身份证 + 住址"
 *
 * <p>想象一个外卖平台：每个餐馆都需要在平台上注册自己的信息——
 * 餐馆叫什么名字（serviceName）、在哪个城市什么版本（serviceVersion）、
 * 门牌号是什么（serviceHost + servicePort）。
 *
 * <p>ServiceMetaInfo 就是 RPC 世界里"服务提供者"的名片。
 * 它被存储在注册中心（如 Etcd、ZooKeeper）中，
 * 消费者通过它来找到并连接到对应的服务提供者。
 *
 * <p>在注册中心中的存储结构（以 Etcd 为例）：
 * <pre>
 *   /rpc/serviceName:version/serviceHost:servicePort  →  JSON(ServiceMetaInfo)
 *
 *   例如：
 *   /rpc/com.hgl.example.common.service.UserService:1.0/192.168.1.100:8080
 * </pre>
 *
 * <p>equals/hashCode 只比较 serviceName + serviceHost + servicePort，
 * 这是语义上的"服务身份"——同一个服务跑在同一个主机端口上，就是同一个实例。
 * serviceVersion 和 serviceGroup 不参与比较，因为同一个实例可以同时服务多个版本。
 *
 * @Author HGL
 * @Create: 2025/9/2 13:41
 */
@Data
@EqualsAndHashCode(of = {"serviceName", "serviceHost", "servicePort"})
public class ServiceMetaInfo {

    /**
     * 服务名称（接口的全限定类名）
     *
     * <p>例如："com.hgl.example.common.service.UserService"
     * 这是服务的"名字"，消费者通过它来指定要调用哪个服务。
     */
    private String serviceName;

    /**
     * 服务版本号
     *
     * <p>默认 "1.0"。用于灰度发布和服务多版本共存。
     * 比如一个新功能上线，可以先部署 v2.0 的服务，
     * 让部分消费者指向 v2.0，其余继续用 v1.0，实现平滑过渡。
     */
    private String serviceVersion = "1.0";

    /**
     * 服务所在的主机地址（IP 或域名）
     *
     * <p>例如："192.168.1.100" 或 "my-service.internal.com"
     * 消费者拿到这个地址后，会用它来建立 TCP/HTTP 连接。
     */
    private String serviceHost;

    /**
     * 服务端口号
     *
     * <p>例如：8080
     * 与 serviceHost 组合，构成完整的网络地址。
     */
    private Integer servicePort;

    /**
     * 服务分组（暂未实现，预留字段）
     *
     * <p>分组用于在同一个服务名下做逻辑隔离。
     * 比如：同一个 UserService，可以有 "dev" 组和 "prod" 组，
     * 开发环境的消费者只调用 dev 组的服务，生产环境调用 prod 组。
     *
     * <p>当前默认值为 "default"，未来可通过 getServiceKey() 扩展。
     */
    private String serviceGroup = "default";

    /**
     * 获取服务键名 —— 服务的"唯一标识"
     *
     * <p>格式：serviceName:serviceVersion
     * 例如："com.hgl.example.common.service.UserService:1.0"
     *
     * <p>这个 key 在注册中心中用于聚合同一个服务的所有节点。
     * 消费者服务发现时，先用这个 key 查找，拿到所有提供者的列表，
     * 再通过负载均衡选择其中一个节点。
     *
     * @return 服务键名
     */
    public String getServiceKey() {
        // 后续可扩展服务分组
        // return String.format("%s:%s:%s", serviceName, serviceVersion, serviceGroup);
        return String.format("%s:%s", serviceName, serviceVersion);
    }

    /**
     * 获取服务注册节点键名 —— 节点在注册中心中的"门牌号"
     *
     * <p>格式：serviceName:serviceVersion/serviceHost:servicePort
     * 例如："com.hgl.example.common.service.UserService:1.0/192.168.1.100:8080"
     *
     * <p>这个 key 是注册中心中每一个具体服务实例的唯一标识。
     * 注册时用它写入，注销时用它删除，心跳续期时用它定位租约。
     *
     * @return 服务注册节点键名
     */
    public String getServiceNodeKey() {
        return String.format("%s/%s:%s", getServiceKey(), serviceHost, servicePort);
    }

    /**
     * 获取完整的服务访问地址（带协议前缀）
     *
     * <p>如果 host 已经包含 "http" 前缀，直接返回 host:port；
     * 否则自动补上 "http://" 前缀。
     *
     * <p>例如：
     *   - host="192.168.1.100", port=8080 → "http://192.168.1.100:8080"
     *   - host="http://my-service.com", port=8080 → "http://my-service.com:8080"
     *
     * @return 带协议前缀的完整服务地址
     */
    public String getServiceAddress() {
        if (!StrUtil.contains(serviceHost, "http")) {
            return String.format("http://%s:%s", serviceHost, servicePort);
        }
        return String.format("%s:%s", serviceHost, servicePort);
    }

}
