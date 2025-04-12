package top.flyingjack.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.openfeign.EnableFeignClients;
import top.flyingjack.common.config.anotation.EnableGlobalCache;
import top.flyingjack.common.config.anotation.EnableGlobalException;
import top.flyingjack.common.config.anotation.EnableGlobalI18n;


@EnableGlobalException
@EnableGlobalI18n
@EnableGlobalCache
@EnableFeignClients
@EnableCaching
@SpringBootApplication(exclude = {RedisAutoConfiguration.class})
public class AuthApplication {
    public static void main(String[] args) throws JsonProcessingException {
        SpringApplication.run(AuthApplication.class, args);
    }
}
