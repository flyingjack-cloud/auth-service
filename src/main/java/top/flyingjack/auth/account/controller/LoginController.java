package top.flyingjack.auth.account.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import top.flyingjack.auth.account.entity.AuthUser;
import top.flyingjack.common.dto.ApiRes;
import top.flyingjack.common.dto.UserDto;
import top.flyingjack.auth.account.entity.dto.UserLoginDto;

@RestController
@RequestMapping("/account")
@Tag(name = "用户登录", description = "包含登录等流程的用户管理接口")
public class LoginController {
    private static final Logger log = LoggerFactory.getLogger(LoginController.class);

    /**
     * 登录入口
     * - 该接口实际不会被调用，只是作为统一展示和doc用，删去不影响逻辑
     * - 具体实逻辑实现请看：
     *      入参 - RestAuthenticationFilter，AuthenticationManager
     *      成功返回 - LoginAuthenticationSuccessHandler
     *
     */
    @PostMapping("/login")
    @Operation(summary = "登录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "401", description = "登录失败", content = @Content),
    })
    public ApiRes<UserDto> login(@Parameter(description = "登录参数", required = true) @RequestBody UserLoginDto userLoginDto) {
        // 认证逻辑将在Filter处理, 为了保证Oauth2的filter能在SecurityContext中直接获取Authentication
        return ApiRes.success();
    }

    @PostMapping("/logout")
    @Operation(summary = "登出账号，注意不是oauth2的登出")
    public ApiRes<Boolean> logout(HttpServletRequest request) {
        try {
            HttpSession session = request.getSession(false); // false表示如果没有session则返回null，不创建新session

            if (session != null) {
                String sessionId = session.getId();
                session.invalidate(); // 销毁Session
                log.debug("Invalidate session: {}", sessionId);
            }
            SecurityContextHolder.clearContext();
            return ApiRes.success(Boolean.TRUE);
        } catch (Exception e) {
            return ApiRes.fail(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to logout");
        }
    }

    @GetMapping("/check-login")
    @Operation(summary = "检查登陆状态，如果登陆返回用户信息")
    public ApiRes<UserDto> checkLogin(HttpServletRequest request) {
        // 1. 获取当前Session
        HttpSession session = request.getSession(false); // false表示如果没有session则返回null，不创建新session

        // 2. 校验Session是否存在
        if (session == null) {
            return ApiRes.fail(HttpStatus.UNAUTHORIZED, "用户未登录");
        }

        // 3. 从Session中获取Spring Security的认证上下文
        SecurityContext securityContext = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);

        // 4. 校验认证上下文和认证信息是否有效
        if (securityContext == null || securityContext.getAuthentication() == null) {
            return ApiRes.fail(HttpStatus.UNAUTHORIZED, "用户未登录");
        }

        Authentication authentication = securityContext.getAuthentication();
        // 5. 校验认证是否已认证（排除匿名认证）
        if (!authentication.isAuthenticated()) {
            return ApiRes.fail(HttpStatus.UNAUTHORIZED, "用户未登录");
        }

        // 6. 获取用户信息并脱敏返回
        if (authentication.getPrincipal() instanceof AuthUser authUser) {
            UserDto userDto = new UserDto(authUser.getId(),
                    authUser.getUsername(),
                    authUser.getPhone(),
                    authUser.getEmail());
            return ApiRes.success(userDto);
        }

        // 其他异常情况
        return ApiRes.fail(HttpStatus.UNAUTHORIZED, "用户未登录");
    }
}
