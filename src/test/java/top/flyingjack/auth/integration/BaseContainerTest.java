package top.flyingjack.auth.integration;

import com.redis.testcontainers.RedisContainer;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * @author Zumin Li
 * @date 2025/4/15 13:26
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("test")
public abstract class BaseContainerTest {
    private static final Logger log = LoggerFactory.getLogger(BaseContainerTest.class);

    protected static final PostgreSQLContainer<?> postgres;

    protected static final RedisContainer redis;

    static {
        postgres = new PostgreSQLContainer<>("postgres:16-alpine")
                .withDatabaseName("test").withInitScript("init-pg.sql");
        redis = new RedisContainer(DockerImageName.parse("redis:6.2.6"));

        postgres.start();
        redis.start();
    }


    @DynamicPropertySource
    static void registerPgProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getRedisPort);
    }

    @BeforeAll
    static void printConnectionInfo() {
        System.out.println("\n========== 测试容器信息 ==========");
        System.out.println("PostgreSQL:");
        System.out.println("  URL: " + postgres.getJdbcUrl());
        System.out.println("  username: " + postgres.getUsername());
        System.out.println("  password: " + postgres.getPassword());
        System.out.println("  Host: " + postgres.getHost());
        System.out.println("  Port: " + postgres.getMappedPort(5432));

        System.out.println("\nRedis:");
        System.out.println("  Host: " + redis.getHost());
        System.out.println("  Port: " + redis.getMappedPort(6379));
        System.out.println("================================\n");
    }
}
