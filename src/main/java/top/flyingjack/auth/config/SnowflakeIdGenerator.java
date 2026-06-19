package top.flyingjack.auth.config;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.IdentifierGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import top.flyingjack.common.tool.SnowflakeIdGeneratorDelegate;

/**
 * Hibernate Id生成器 - 交给spring托管
 * ex:
 *     @Id
 *     @GeneratedValue(generator = "snowflake-id-generator")
 *     @GenericGenerator(name = "snowflake-id-generator", type = SnowflakeIdGenerator.class)
 *     private Long id;
 *
 * @author Zumin Li
 * @date 2025/4/16 14:41
 */
@Component
public class SnowflakeIdGenerator implements IdentifierGenerator {
    private final SnowflakeIdGeneratorDelegate delegate;

    private long datacenterId;
    private long machineId;

    public SnowflakeIdGenerator(@Value("${snowflake.datacenter-id}") String datacenterId,
                                @Value("${snowflake.machine-id}") String machineId) {
        // 转换DataCenterID：使用节点名称的哈希值
        this.datacenterId = convertNodeNameToId(datacenterId);

        // 转换MachineID：如果是StatefulSet的Pod名称，提取序号；否则用哈希
        this.machineId = convertPodNameToId(machineId);

        this.delegate = new SnowflakeIdGeneratorDelegate(this.datacenterId, this.machineId);
    }

    // 从k8s环境中获取的需要进行转换
    private long convertNodeNameToId(String nodeName) {
        if (nodeName == null || nodeName.isEmpty()) {
            return 0L;
        }

        // 1. 首先尝试直接转换为long
        try {
            return Long.parseLong(nodeName) % ((long) 31 + 1);
        } catch (NumberFormatException e) {
            // 2. 转换失败，使用哈希值
            return Math.abs(nodeName.hashCode()) % ((long) 31 + 1);
        }
    }

    // 从k8s环境中获取的需要进行转换
    private long convertPodNameToId(String podName) {
        if (podName == null || podName.isEmpty()) {
            return 0L;
        }

        // 1. 首先尝试直接转换为long
        try {
            return Long.parseLong(podName) % ((long) 31 + 1);
        } catch (NumberFormatException e) {
            // 2. 转换失败，检查是否是StatefulSet格式(名称-数字)
            try {
                String[] parts = podName.split("-");
                if (parts.length > 1) {
                    String numberStr = parts[parts.length - 1];
                    return Long.parseLong(numberStr) % ((long) 31 + 1);
                }
            } catch (NumberFormatException ex) {
                // 不是StatefulSet格式，继续处理
            }

            // 3. 最后使用哈希值
            return Math.abs(podName.hashCode()) % ((long) 31 + 1);
        }
    }

    @Override
    public Object generate(SharedSessionContractImplementor sharedSessionContractImplementor, Object o) {
        return delegate.nextId();
    }

    public SnowflakeIdGeneratorDelegate getDelegate() {
        return delegate;
    }
}
