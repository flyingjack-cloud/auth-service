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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.authorization.OAuth2Authorization;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2TokenType;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.util.StringUtils;
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

    private final OAuth2AuthorizationService authorizationService;

    public LoginController(OAuth2AuthorizationService authorizationService) {
        this.authorizationService = authorizationService;
    }

    /**
     * 登录入口 — 实际由 RestAuthenticationFilter 处理，此方法仅作 Swagger 文档占位。
     */
    @PostMapping("/login")
    @Operation(summary = "登录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "401", description = "登录失败", content = @Content),
    })
    public ApiRes<UserDto> login(@Parameter(description = "登录参数", required = true)
                                 @RequestBody UserLoginDto userLoginDto) {
        return ApiRes.success();
    }

    @PostMapping("/logout")
    @Operation(summary = "登出：清除 Session 并吊销当前 OAuth2 AccessToken（如存在）")
    public ApiRes<Boolean> logout(HttpServletRequest request) {
        try {
            // 吊销 Authorization header 中携带的 Bearer AccessToken
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                OAuth2Authorization authorization =
                        authorizationService.findByToken(token, OAuth2TokenType.ACCESS_TOKEN);
                if (authorization != null) {
                    authorizationService.remove(authorization);
                    log.debug("Revoked OAuth2 access token for principal: {}", authorization.getPrincipalName());
                }
            }

            // 销毁 Session
            HttpSession session = request.getSession(false);
            if (session != null) {
                log.debug("Invalidate session: {}", session.getId());
                session.invalidate();
            }
            SecurityContextHolder.clearContext();
            return ApiRes.success(Boolean.TRUE);
        } catch (Exception e) {
            log.error("Logout failed", e);
            return ApiRes.fail(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to logout");
        }
    }

    @GetMapping("/check-login")
    @Operation(summary = "检查登录状态，如果已登录则返回用户信息")
    public ApiRes<UserDto> checkLogin(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ApiRes.fail(HttpStatus.UNAUTHORIZED, "用户未登录");
        }

        SecurityContext securityContext = (SecurityContext) session.getAttribute(
                HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY);

        if (securityContext == null || securityContext.getAuthentication() == null) {
            return ApiRes.fail(HttpStatus.UNAUTHORIZED, "用户未登录");
        }

        Authentication authentication = securityContext.getAuthentication();
        if (!authentication.isAuthenticated()) {
            return ApiRes.fail(HttpStatus.UNAUTHORIZED, "用户未登录");
        }

        if (authentication.getPrincipal() instanceof AuthUser authUser) {
            return ApiRes.success(new UserDto(authUser.getId(), authUser.getUsername(),
                    authUser.getPhone(), authUser.getEmail()));
        }

        return ApiRes.fail(HttpStatus.UNAUTHORIZED, "用户未登录");
    }
}
