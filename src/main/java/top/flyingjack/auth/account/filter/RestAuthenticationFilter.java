package top.flyingjack.auth.account.filter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.util.StringUtils;
import top.flyingjack.auth.account.entity.dto.UserLoginDto;
import top.flyingjack.auth.account.service.LoginAttemptService;
import top.flyingjack.auth.feign.client.CaptchaClient;
import top.flyingjack.common.dto.CaptchaRequest;
import top.flyingjack.common.error.exception.AttemptAuthenticationException;
import top.flyingjack.common.error.exception.CaptchaAuthenticationException;
import top.flyingjack.common.tool.HttpTools;
import top.flyingjack.common.tool.Verify;

/**
 * 登录验证用Filter（带验证码检查功能） - 替换默认的UsernamePasswordAuthenticationFilter
 */
public class RestAuthenticationFilter extends UsernamePasswordAuthenticationFilter {
    public static final int MAX_ATTEMPT = 3; // 超过多少次尝试需要验证码
    public static final int BLOCK_ATTEMPT = 10; // 超过多少次尝试锁定用户

    public static final String CAPTCHA_ID_HEADER = "X-Captcha-ID";
    public static final String CAPTCHA_TOKEN_HEADER = "X-Captcha-Token";

    private LoginAttemptService loginAttemptService;
    private CaptchaClient captchaClient;

    /**
     * 重写验证过程，支持Json格式的LoginRequest请求体
     */
    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response) throws AuthenticationException {
        // 只接受post请求
        if (!request.getMethod().equals("POST")) {
            throw new InsufficientAuthenticationException("Only support POST method in login.");
        }

        UserLoginDto userLoginDto;
        // 解压请求，可能会失败
        try {
            userLoginDto = HttpTools.parseJsonToObject(request, UserLoginDto.class);
        } catch (Exception e) {
            throw new InsufficientAuthenticationException("Failed to parse authentication request body");
        }

        captchaCheck(request, userLoginDto);

        UsernamePasswordAuthenticationToken authRequest =
                UsernamePasswordAuthenticationToken.unauthenticated(userLoginDto.principal(), userLoginDto.password());
        // 将登录类型放进detail，这样后续就可以支持判断principal类型支持多种登录
        authRequest.setDetails(userLoginDto.loginType());
        return this.getAuthenticationManager().authenticate(authRequest);
    }

    /**
     * 检查登录信息, 基本逻辑是
     * - 先检查是否需要验证码
     * - 如果需要检查头中是否有对应的信息
     * - 最后在调用服务检查验证码
     *
     */
    private void captchaCheck(HttpServletRequest request, UserLoginDto userLoginDto) {
        int failedCount = this.loginAttemptService.count(userLoginDto.principal());
        // 如果之前超过了尝试上限，直接返回异常
        if (failedCount >= BLOCK_ATTEMPT) {
            throw new AttemptAuthenticationException("To many attempt, {} is blocked",
                    this.loginAttemptService.expireRemain(userLoginDto.principal()));
        } else if (failedCount >= MAX_ATTEMPT) {
            // 验证码检验
            String captchaId = request.getHeader(CAPTCHA_ID_HEADER);
            String captchaToken = request.getHeader(CAPTCHA_TOKEN_HEADER);

            // 头检查
            if (!StringUtils.hasLength(captchaId) || !StringUtils.hasLength(captchaToken) ) {
                logger.debug("Missing X-Captcha-ID/X-Captcha-Token header");
                this.loginAttemptService.record(userLoginDto.principal()); // 抛出验证码相关异常前，都要记录一次尝试次数
                throw new CaptchaAuthenticationException("Missing X-Captcha-ID/X-Captcha-Token header");
            } else {
                // 验证合法性
                if (!Verify.verifyCaptchaId(captchaId)) {
                    logger.debug("Invalid X-Captcha-ID");
                    this.loginAttemptService.record(userLoginDto.principal());
                    throw new CaptchaAuthenticationException("Invalid X-Captcha-ID");
                }

                // 实际验证过程
                boolean result = this.captchaClient.verify(new CaptchaRequest(captchaId, captchaToken));

                // 如果验证码验证成功了，清除记录，继续其他filter流程
                if (result) {
                    this.loginAttemptService.clear(userLoginDto.principal());
                } else {
                    this.loginAttemptService.record(userLoginDto.principal());
                    throw new CaptchaAuthenticationException("Captcha verify failed");
                }
            }
        }
    }

    public void setCaptchaClient(CaptchaClient captchaClient) {
        this.captchaClient = captchaClient;
    }

    public void setLoginAttemptService(LoginAttemptService loginAttemptService) {
        this.loginAttemptService = loginAttemptService;
    }
}
