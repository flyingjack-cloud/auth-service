package top.flyingjack.auth.account.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import top.flyingjack.common.error.ErrorRes;
import top.flyingjack.common.error.GlobalExceptionHandler;
import top.flyingjack.common.tool.ResponseUtils;

import java.io.IOException;

/**
 *  Spring Security下的登录期间错误（UsernamePasswordFilter）
 *  - 401 登录出错
 *
 * @author Zumin Li
 * @date 2025/4/5 19:47
 */
@Component
public class LoginAuthenticationFailureHandler implements AuthenticationFailureHandler {
    private static final Logger log = LoggerFactory.getLogger(LoginAuthenticationFailureHandler.class);

    private GlobalExceptionHandler globalExceptionHandler;

    public LoginAuthenticationFailureHandler(GlobalExceptionHandler globalExceptionHandler) {
        this.globalExceptionHandler = globalExceptionHandler;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        log.debug("Authenticated failed: {}", exception.getMessage());

        // 调用全局异常处理器的认证异常处理方法
        ResponseEntity<ErrorRes> result = globalExceptionHandler.handleAuthenticationException(
                exception, request);

        if (result.getBody() != null){
            ResponseUtils.writeErrorRes(response, result.getBody());
        }
    }
}