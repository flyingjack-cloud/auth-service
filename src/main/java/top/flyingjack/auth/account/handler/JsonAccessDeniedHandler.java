package top.flyingjack.auth.account.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import top.flyingjack.common.error.ErrorRes;
import top.flyingjack.common.error.GlobalExceptionHandler;
import top.flyingjack.common.tool.ResponseUtils;

import java.io.IOException;

/**
 * Spring Security下的Access Denied Exception处理 - 403
 *
 * @author Zumin Li
 * @date 2025/4/5 13:11
 */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {
    private static final Logger log = LoggerFactory.getLogger(JsonAccessDeniedHandler.class);

    private GlobalExceptionHandler globalExceptionHandler;

    public JsonAccessDeniedHandler(GlobalExceptionHandler globalExceptionHandler) {
        this.globalExceptionHandler = globalExceptionHandler;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.debug("Access denied under path {} with {}", request.getRequestURI(), accessDeniedException.getMessage());

        // 调用全局异常处理器的权限异常处理方法
        ResponseEntity<ErrorRes> result = globalExceptionHandler.handleAccessDeniedException(
                accessDeniedException, request);

        if (result.getBody() != null){
            ResponseUtils.writeErrorRes(response, result.getBody());
        }
    }
}
