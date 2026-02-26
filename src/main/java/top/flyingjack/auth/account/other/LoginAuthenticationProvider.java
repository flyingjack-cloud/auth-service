package top.flyingjack.auth.account.other;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.Assert;
import top.flyingjack.auth.account.entity.PrincipalType;
import top.flyingjack.auth.account.service.LoginAttemptService;
import top.flyingjack.auth.account.service.LoginUserDetailService;
import top.flyingjack.common.error.ErrorCode;
import top.flyingjack.common.error.SysErrorMsgTool;

import java.math.BigInteger;


/**
 * 支持多种身份的登录认证provider
 * - tip - 如果需要实现额外的检查，请在preAuthenticationChecks，credentialAuthenticationChecks和postAuthenticationChecks中实现
 *
 * @author Zumin Li
 * @date 2025/4/2 23:14
 */
public class LoginAuthenticationProvider implements AuthenticationProvider, InitializingBean {
    private static final Logger logger = LoggerFactory.getLogger(LoginAuthenticationProvider.class);

    private PasswordEncoder passwordEncoder;
    private LoginUserDetailService userDetailsService;
    private LoginAttemptService loginAttemptService;

    public LoginAuthenticationProvider(LoginUserDetailService userDetailsService,
                                       PasswordEncoder passwordEncoder,
                                       LoginAttemptService loginAttemptService) {
        this.userDetailsService = userDetailsService;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
    }

    // 检查器，用于检查用户信息
    private UserDetailsChecker preAuthenticationChecks =
            new LoginAuthenticationProvider.DefaultPreAuthenticationChecks();
    private UserDetailsChecker postAuthenticationChecks =
            new LoginAuthenticationProvider.DefaultPostAuthenticationChecks();

    /**
     * The plaintext password used to perform
     * {@link PasswordEncoder#matches(CharSequence, String)} on when the user is not found
     * to avoid SEC-2056.
     */
    private static final String USER_NOT_FOUND_PASSWORD = "userNotFoundPassword";

    /**
     * The password used to perform {@link PasswordEncoder#matches(CharSequence, String)}
     * on when the user is not found to avoid SEC-2056. This is necessary, because some
     * {@link PasswordEncoder} implementations will short circuit if the password is not
     * in a valid format.
     */
    private volatile String userNotFoundEncodedPassword;

    @Override
    public final void afterPropertiesSet() throws Exception {
        Assert.notNull(this.userDetailsService, SysErrorMsgTool.fromError(
                ErrorCode.SYSTEM_ERROR, "UserDetailsService must be set"));
    }

    /**
     * 验证身份信息，验证过程主要逻辑
     *
     * @param authentication the authentication request object.
     * @throws AuthenticationException
     */
    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        Assert.isInstanceOf(UsernamePasswordAuthenticationToken.class, authentication,
                SysErrorMsgTool.fromError(ErrorCode.AUTHENTICATE_FAILED,
                        "LoginAuthenticationProvider.onlySupports Only UsernamePasswordAuthenticationToken is " +
                                "supported"));

        // 从前序token中解压用户识别信息
        PrincipalType loginType = null;
        try {
            loginType = (PrincipalType) authentication.getDetails();
        } catch (ClassCastException e) {
            logger.debug("Request from {} dont have a mismatched type of loginType - {}", authentication.getPrincipal(),
                    authentication.getDetails());
        }

        if (loginType == null || authentication.getPrincipal() == null) {
            throw new InsufficientAuthenticationException(SysErrorMsgTool.fromError(ErrorCode.INSUFFICIENT,
                    "Request dont have a valid login request"));
        }

        // 从service中拉取用户
        UserDetails user = retrieveUser(loginType, authentication.getPrincipal()
                .toString(), (UsernamePasswordAuthenticationToken) authentication);
        Assert.notNull(user, "retrieveUser returned null - a violation of the interface contract");

        // 下面是验证过程
        this.preAuthenticationChecks.check(user);
        this.credentialAuthenticationChecks(user, (UsernamePasswordAuthenticationToken) authentication);
        this.postAuthenticationChecks.check(user);

        return createSuccessAuthentication(user, authentication, user);
    }

    /**
     * Creates a successful {@link Authentication} object.
     * <p>
     * Protected so subclasses can override.
     * </p>
     * <p>
     * Subclasses will usually store the original credentials the user supplied (not
     * salted or encoded passwords) in the returned <code>Authentication</code> object.
     * </p>
     *
     * @param principal      that should be the principal in the returned object
     * @param authentication that was presented to the provider for validation
     * @param user           that was loaded by the implementation
     * @return the successful authentication token
     */
    protected Authentication createSuccessAuthentication(Object principal, Authentication authentication,
                                                         UserDetails user) {
        // Ensure we return the original credentials the user supplied,
        // so subsequent attempts are successful even with encoded passwords.
        // Also ensure we return the original getDetails(), so that future
        // authentication events after cache expiry contain the details
        UsernamePasswordAuthenticationToken result = UsernamePasswordAuthenticationToken.authenticated(principal,
                authentication.getCredentials(), user.getAuthorities());
        result.setDetails(authentication.getDetails());
        logger.debug("Authenticated user");
        return result;
    }

    /**
     * 根据登录类型拉取用户
     *
     * @param loginType      登录类型
     * @param principal      身份信息
     * @param authentication filter传入
     * @throws AuthenticationException
     */
    private UserDetails retrieveUser(PrincipalType loginType,
                                     String principal,
                                     UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        prepareTimingAttackProtection();

        try {
            // 根据用户类型开始查找用户
            UserDetails loadedUser = null;
            if (loginType == PrincipalType.PHONE) {
                loadedUser = this.userDetailsService.loadUserByPhone(principal);
            } else if (loginType == PrincipalType.EMAIL) {
                loadedUser = this.userDetailsService.loadUserByEmail(principal);
            } else if (loginType == PrincipalType.USERNAME) {
                loadedUser = this.userDetailsService.loadUserByUsername(principal);
            }

            if (loadedUser == null) {
                throw new InternalAuthenticationServiceException(
                        SysErrorMsgTool.fromError(ErrorCode.AUTHENTICATE_FAILED, "UserDetailsService returned " +
                                "null, which is an interface contract violation")
                );
            }
            return loadedUser;
        } catch (UsernameNotFoundException ex) {
            mitigateAgainstTimingAttack(authentication);

            logger.debug("Load user failed - {}", ex.getMessage());
            // 为了不暴露信息，这里统一都设置为一种错误
            throw new UsernameNotFoundException(
                    SysErrorMsgTool.fromError(ErrorCode.BAD_CREDENTIAL, "User Not found")
            );
        } catch (InternalAuthenticationServiceException ex) {
            logger.debug("User {} not found.", principal);
            throw new UsernameNotFoundException(
                    SysErrorMsgTool.fromError(ErrorCode.BAD_CREDENTIAL, "User Not found"));
        } catch (Exception ex) {
            throw new InternalAuthenticationServiceException(ex.getMessage(), ex);
        }
    }

    /**
     * 登录密码验证
     */
    private void credentialAuthenticationChecks(UserDetails userDetails,
                                                UsernamePasswordAuthenticationToken authentication) throws AuthenticationException {
        {
            boolean isPassed = true;

            if (authentication.getCredentials() == null) {
                logger.debug("Failed to authenticate since no credentials provided");
                isPassed = false;
            } else {
                String presentedPassword = authentication.getCredentials().toString();
                // 进行密码验证
                if (!this.passwordEncoder.matches(presentedPassword, userDetails.getPassword())) {
                    logger.debug("Failed to authenticate since password does not match stored value");
                    isPassed = false;
                }
            }

            if (!isPassed) {
                // 记录失败登录
                this.loginAttemptService.record(authentication.getPrincipal().toString());
                throw new BadCredentialsException(SysErrorMsgTool.fromError(ErrorCode.BAD_CREDENTIAL, "Bad " +
                        "credentials"));
            }
        }
    }

    /**
     * 预检查器, 检查账户是否过期，锁定，开启
     */
    private class DefaultPreAuthenticationChecks implements UserDetailsChecker {
        @Override
        public void check(UserDetails user) {
            if (!user.isAccountNonLocked()) {
                logger.debug("Failed to authenticate since user account is locked");
                throw new LockedException(SysErrorMsgTool.fromError(ErrorCode.INVALID_ACCOUNT, "User account is " +
                        "locked"));
            }
            if (!user.isEnabled()) {
                logger.debug("Failed to authenticate since user account is disabled");
                throw new DisabledException(SysErrorMsgTool.fromError(ErrorCode.INVALID_ACCOUNT, "User is disabled"));
            }
            if (!user.isAccountNonExpired()) {
                logger.debug("Failed to authenticate since user account has expired");
                throw new AccountExpiredException(SysErrorMsgTool.fromError(ErrorCode.INVALID_ACCOUNT, "User account " +
                        "has expired"));
            }
        }
    }

    /**
     * 后置检查器，检查密码是否过期
     */
    private class DefaultPostAuthenticationChecks implements UserDetailsChecker {
        @Override
        public void check(UserDetails user) {
            if (!user.isCredentialsNonExpired()) {
                logger.debug("Failed to authenticate since user account credentials have expired");
                throw new CredentialsExpiredException(SysErrorMsgTool.fromError(ErrorCode.EXPIRED_CREDENTIAL, "User " +
                        "credentials have expired"));
            }
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return (UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication));
    }


    private void prepareTimingAttackProtection() {
        if (this.userNotFoundEncodedPassword == null) {
            this.userNotFoundEncodedPassword = this.passwordEncoder.encode(USER_NOT_FOUND_PASSWORD);
        }
    }

    private void mitigateAgainstTimingAttack(UsernamePasswordAuthenticationToken authentication) {
        if (authentication.getCredentials() != null) {
            String presentedPassword = authentication.getCredentials().toString();
            this.passwordEncoder.matches(presentedPassword, this.userNotFoundEncodedPassword);
        }
    }
}
