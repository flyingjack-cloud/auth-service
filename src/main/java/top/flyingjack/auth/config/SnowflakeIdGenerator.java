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

    public SnowflakeIdGenerator(@Value("${snowflake.datacenter-id}") long datacenterId,
                                @Value("${snowflake.machine-id}") long machineId) {
        this.delegate = new SnowflakeIdGeneratorDelegate(datacenterId, machineId);
    }

    @Override
    public Object generate(SharedSessionContractImplementor sharedSessionContractImplementor, Object o) {
        return delegate.nextId();
    }
}
