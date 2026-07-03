package com.hgl.hglrpc.loadbalancer;

import com.hgl.hglrpc.model.ServiceMetaInfo;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * ConsistentHashLoadBalancer —— 一致性哈希负载均衡器（"按门牌号派件的调度员"）
 *
 * <h2>核心思想：同一个地址的包裹 always 交给同一个快递员</h2>
 * <p>
 * 想象一个神奇的<b>圆环</b>（哈希环），快递员们均匀分布在环上。
 * 每当一个新包裹到来，调度员根据包裹上的<b>收件地址</b>（请求参数）计算出一个角度，
 * 然后<b>顺时针找到最近的快递员</b>，把包裹交给他。
 * 因为同一个地址总是算出同一个角度，所以同一个地址的包裹<b>永远</b>交给同一个快递员。
 * </p>
 *
 * <h2>一致性哈希环示意图</h2>
 * <pre>
 *                    0 / 2^32
 *                       │
 *              S1-v2 ●  │
 *             ╱          ╲
 *           ╱              ╲
 *    S3-v1 ●                ● S1-v1
 *         │                  │
 *         │    哈希环        │
 *         │   (顺时针)       │
 *    S3-v0 ●                ● S2-v2
 *           ╲              ╱
 *             ╲          ╱
 *              S2-v0 ●  │
 *                       │
 *
 *  请求 X 的 hash 落在 S1-v1 和 S2-v2 之间
 *  → 顺时针找到 S2-v2 → 交给 S2 节点处理
 *
 *  注意：真实环境中每个物理节点有 100 个虚拟节点（上图仅示意）
 * </pre>
 *
 * <h2>为什么需要虚拟节点？</h2>
 * <p>
 * 如果只有 3 台物理服务器，只在环上放 3 个点，
 * 分布可能极不均匀——可能出现某个弧段占了 70% 的环长。
 * <b>虚拟节点</b>的思路是：每台物理服务器"分身"成 100 个虚拟节点，
 * 均匀散布在环上。这样 3 台服务器就有 300 个点，分布就非常均匀了。
 * </p>
 * <pre>
 *  没有虚拟节点（3 个点）：      有虚拟节点（每台 100 个）：
 *      S1●                       ● ● ● ● ● ● ●
 *     ╱    ╲                    ●  S1 散布  ●
 *   ╱        ╲                  ● ● ● ● ● ● ●
 *  ● S3       ● S2              ●  S2 散布  ●
 *   ╲        ╱                  ● ● ● ● ● ● ●
 *     ╲    ╱                    ●  S3 散布  ●
 *                               ● ● ● ● ● ● ●
 *  (负载可能极不均匀)           (负载非常均匀)
 * </pre>
 *
 * <h2>适用场景</h2>
 * <ul>
 *   <li><b>缓存亲和性</b>：同一个用户（或同一类数据）的请求总是打到同一台机器，
 *       命中本地缓存，减少缓存穿透</li>
 *   <li><b>有状态会话</b>：需要把同一客户端的请求路由到持有其会话的节点</li>
 *   <li><b>分布式 Session</b>：同一用户的 Session 存在固定节点上</li>
 * </ul>
 *
 * <h2>节点变动时的影响</h2>
 * <p>
 * 当某个节点宕机或新增节点时，一致性哈希的优势就体现出来了——
 * 只有该节点（及其虚拟节点）负责的那小段区间需要重新映射，
 * 其他节点的映射关系<b>不受影响</b>。
 * 这就是"一致性"的含义：节点变动时，数据迁移量最小。
 * </p>
 *
 * @author HGL
 * @see LoadBalancer
 * @see LoadBalancerKeys#CONSISTENT_HASH
 */
public class ConsistentHashLoadBalancer implements LoadBalancer {

    /**
     * 一致性哈希环——用 TreeMap 模拟环形结构
     * <p>
     * key:   虚拟节点的哈希值（int 范围，视为环上的"刻度"）
     * value: 该虚拟节点对应的物理服务节点信息
     * </p>
     * <p>
     * 为什么用 TreeMap？
     * TreeMap 底层是红黑树，支持 {@code ceilingEntry(key)}——
     * 查找"大于等于 key 的最小 entry"，时间复杂度 O(log N)。
     * 这正好对应哈希环上的"顺时针找最近节点"操作。
     * 如果用 HashMap，则需要遍历所有 key 才能找到，O(N) 太慢。
     * </p>
     */
    private final TreeMap<Integer, ServiceMetaInfo> virtualNodes = new TreeMap<>();

    /**
     * 每个物理节点的虚拟节点数——"每个快递员在环上插 100 面旗帜"
     * <p>
     * 数量越多，分布越均匀，但内存消耗和构建时间也越多。
     * 100 是一个经验值，在大多数场景下平衡了均匀性和性能。
     * </p>
     */
    private static final int VIRTUAL_NODE_NUM = 100;

    /**
     * 使用一致性哈希策略选择服务节点——"按收件地址找到最近的快递员"
     *
     * @param requestParams       请求参数，用于计算哈希值确定在环上的位置
     * @param serviceMetaInfoList 可用服务列表
     * @return 被选中的服务节点；列表为空时返回 {@code null}
     */
    @Override
    public ServiceMetaInfo select(Map<String, Object> requestParams, List<ServiceMetaInfo> serviceMetaInfoList) {
        if (serviceMetaInfoList.isEmpty()) {
            return null;
        }

        // ========== 第一步：构建虚拟节点环 ==========
        // 把每个物理服务节点"分身"成 VIRTUAL_NODE_NUM 个虚拟节点，
        // 放到 TreeMap 中（按哈希值排序，形成"环"的结构）
        for (ServiceMetaInfo serviceMetaInfo : serviceMetaInfoList) {
            for (int i = 0; i < VIRTUAL_NODE_NUM; i++) {
                // 用 "服务地址#编号" 作为哈希的 key
                // 例如 "192.168.1.10:8080#0", "192.168.1.10:8080#1", ...
                // 不同编号产生不同哈希值，模拟环上不同的"旗帜位置"
                int hash = getHash(serviceMetaInfo.getServiceAddress() + "#" + i);
                virtualNodes.put(hash, serviceMetaInfo);
            }
        }

        // ========== 第二步：计算请求的哈希值 ==========
        // 根据请求参数（如用户ID、请求路径等）计算哈希值，
        // 确定这个请求在环上的"落点"
        int hash = getHash(requestParams);

        // ========== 第三步：顺时针查找最近的虚拟节点 ==========
        // ceilingEntry(hash) 返回 key >= hash 的最小 Entry，
        // 即"从当前落点顺时针走，遇到的第一个节点"
        Map.Entry<Integer, ServiceMetaInfo> entry = virtualNodes.ceilingEntry(hash);
        if (entry == null) {
            // 如果没有 >= hash 的节点，说明落点在环的末尾
            // 此时需要"绕回"到环的起点——即第一个节点
            // 这就是"环形"的精髓：末尾连着开头
            entry = virtualNodes.firstEntry();
        }
        return entry.getValue();
    }

    /**
     * 计算对象的哈希值——"计算包裹在环上的落点角度"
     * <p>
     * 当前直接使用 {@code Object.hashCode()}，简单但分布可能不够均匀。
     * 在生产环境中，建议替换为 FNV-1a、MurmurHash 等分布更均匀的哈希算法，
     * 以避免哈希冲突导致的负载倾斜。
     * </p>
     *
     * @param key 要计算哈希的对象（请求参数或虚拟节点标识）
     * @return 哈希值（int 范围，映射到环上的位置）
     */
    private int getHash(Object key) {
        return key.hashCode();
    }
}
