package top.flyingjack.auth.account.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Component;
import top.flyingjack.auth.account.entity.AuthUser;
import top.flyingjack.common.dto.ApiRes;
import top.flyingjack.common.dto.UserDto;
import top.flyingjack.common.tool.ResponseUtils;

import java.io.IOException;


/**
 * 兑换令牌的AuthenticationSuccessHandler
 *
 * @author Zumin Li
 * @date 2025/4/1 23:35
 */
@Component
public class LoginAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
    /**
     * 默认的onAuthenticationSuccess会发送重定位请求，我们这里什么都不做
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        // 保存登录状态到session
        SecurityContextHolder.getContext().setAuthentication(authentication);
        request.getSession().setAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY,
                SecurityContextHolder.getContext()
        );

        // 返回前需要重新包装隐去敏感信息
        if (authentication.getPrincipal() instanceof AuthUser authUser) {
            UserDto userDto = new UserDto(authUser.getId(),
                    authUser.getUsername(),
                    authUser.getPhone(),
                    authUser.getEmail());

            ResponseUtils.writeJsonResponse(response, ApiRes.success(userDto));
        }
    }
}
