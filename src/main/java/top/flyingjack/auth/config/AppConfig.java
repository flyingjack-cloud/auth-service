package top.flyingjack.auth.config;

import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.hibernate.id.IdentifierGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import top.flyingjack.common.tool.FlexibleInstantDeserializer;
import top.flyingjack.common.tool.SnowflakeIdGeneratorDelegate;

import java.time.Instant;
import java.util.Locale;

/**
 * 其他Spring相关配置
 *
 * @author Zumin Li
 * @date 2025/4/4 21:59
 */
@Configuration
@EnableRedisHttpSession
@EnableTransactionManagement
public class AppConfig {
    /**
     * Accept-language 请求头locale解析
     */
    @Bean
    public LocaleResolver localeResolver() {
        AcceptHeaderLocaleResolver resolver = new AcceptHeaderLocaleResolver();
        resolver.setDefaultLocale(Locale.US);
        return resolver;
    }

    @Bean
    public Module instantModule(){
        JavaTimeModule instantModule = new JavaTimeModule();
        // 注册自定义的Instant反序列器
        instantModule.addDeserializer(Instant.class, new FlexibleInstantDeserializer());
        return instantModule;
    }
}
