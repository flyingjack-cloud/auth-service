package top.flyingjack.auth.account.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import top.flyingjack.common.error.ErrorRes;
import top.flyingjack.common.error.GlobalExceptionHandler;
import top.flyingjack.common.tool.ResponseUtils;

import java.io.IOException;

/**
 *  Spring Security下的未认证访问统一处理
 *  - 匿名认证访问时
 *  - 访问受保护资源时未携带认证信息
 *  - 401 未登录/令牌无效
 *
 * @author Zumin Li
 * @date 2025/4/5 13:24
 */
@Component
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private static final Logger log = LoggerFactory.getLogger(JsonAuthenticationEntryPoint.class);

    private GlobalExceptionHandler globalExceptionHandler;

    public JsonAuthenticationEntryPoint(GlobalExceptionHandler globalExceptionHandler) {
        this.globalExceptionHandler = globalExceptionHandler;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        log.debug("Unauthorized: {}", authException.getMessage());

        // 调用全局异常处理器的认证异常处理方法
        ResponseEntity<ErrorRes> result = globalExceptionHandler.handleAuthenticationException(
                authException, request);

        if (result.getBody() != null){
            ResponseUtils.writeErrorRes(response, result.getBody());
        }
    }
}
