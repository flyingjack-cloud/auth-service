package top.flyingjack.auth.feign;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.flyingjack.common.tool.HttpTools;

@Configuration
public class FeignConfig {

    @Bean
    public RequestInterceptor ipForwardInterceptor() {
        return template -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                String clientIp = HttpTools.getClientIp(request);
                if (clientIp != null && !clientIp.isEmpty()) {
                    template.header("X-Forwarded-For", clientIp);
                }
            }
        };
    }
}
