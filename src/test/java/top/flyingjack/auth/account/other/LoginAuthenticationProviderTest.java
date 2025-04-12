package top.flyingjack.auth.account.other;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import top.flyingjack.auth.CommonTestData;
import top.flyingjack.auth.account.entity.PrincipalType;
import top.flyingjack.auth.account.service.LoginAttemptService;

/**
 * LoginAuthenticationProvider测试类
 * - 主要检查自定义的authenticate
 *
 * @author Zumin Li
 * @date 2025/4/6 0:03
 */
class LoginAuthenticationProviderTest {
    private CommonTestData testData = new CommonTestData();

    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private LoginAuthenticationProvider authenticationProvider;

    private LoginAttemptService loginAttemptService = Mockito.mock(LoginAttemptService.class);

    @BeforeEach
    void setUp() {
        authenticationProvider = new LoginAuthenticationProvider(this.testData.LOGIN_USER_DETAIL_SERVICE,
                passwordEncoder, loginAttemptService);
    }

    @AfterEach
    void tearDown() {
        // 每次使用完后都要重置service
        this.testData.resetUpLoginUserDetailService();
    }

    /**
     * 传入错误token时测试authenticate()
     */
    @Test
    public void should_throw_exception_when_token_is_not_compatible() {
        Authentication token = Mockito.mock(Authentication.class);
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.authenticationProvider.authenticate(token));
    }

    /**
     * 传入错误不合法登录信息时测试authenticate()
     */
    @Test
    public void should_throw_exception_when_using_invalid_login_info() {
        // 1. LoginType问题测试
        final UsernamePasswordAuthenticationToken token = UsernamePasswordAuthenticationToken.unauthenticated(
                this.testData.VALID_USERNAME, this.testData.VALID_PASSWORD
        );
        // 未传入loginType到details时
        Assertions.assertThrows(InsufficientAuthenticationException.class, () -> this.authenticationProvider.authenticate(token));
        // loginType类型错误时
        token.setDetails(this.testData);
        Assertions.assertThrows(InsufficientAuthenticationException.class, () -> this.authenticationProvider.authenticate(token));

        // 2. 不存在的principal时测试
        final UsernamePasswordAuthenticationToken phoneToken = UsernamePasswordAuthenticationToken.unauthenticated(
                this.testData.INVALID_PHONE, this.testData.VALID_PASSWORD
        );
        phoneToken.setDetails(PrincipalType.PHONE);
        Assertions.assertThrows(UsernameNotFoundException.class, () -> this.authenticationProvider.authenticate(phoneToken));

        final UsernamePasswordAuthenticationToken emailToken = UsernamePasswordAuthenticationToken.unauthenticated(
                this.testData.INVALID_EMAIL, this.testData.VALID_PASSWORD
        );
        emailToken.setDetails(PrincipalType.EMAIL);
        Assertions.assertThrows(UsernameNotFoundException.class, () -> this.authenticationProvider.authenticate(emailToken));

        final UsernamePasswordAuthenticationToken usernameToken = UsernamePasswordAuthenticationToken.unauthenticated(
                this.testData.INVALID_USERNAME, this.testData.VALID_PASSWORD
        );
        usernameToken.setDetails(PrincipalType.USERNAME);
        Assertions.assertThrows(UsernameNotFoundException.class, () -> this.authenticationProvider.authenticate(usernameToken));
    }

    /**
     * 密码错误测试
     */
    @Test
    public void should_throw_exception_when_using_invalid_credentials() {
        Mockito.reset(this.loginAttemptService);

        final UsernamePasswordAuthenticationToken token1 = UsernamePasswordAuthenticationToken.unauthenticated(
                this.testData.VALID_USERNAME, null
        );
        token1.setDetails(PrincipalType.USERNAME);
        Assertions.assertThrows(BadCredentialsException.class, () -> this.authenticationProvider.authenticate(token1));
        Mockito.verify(this.loginAttemptService, Mockito.times(1)).record(this.testData.VALID_USERNAME); // 是否记录了登录失败

        final UsernamePasswordAuthenticationToken token2 = UsernamePasswordAuthenticationToken.unauthenticated(
                this.testData.VALID_USERNAME, ""
        );
        token2.setDetails(PrincipalType.USERNAME);
        Assertions.assertThrows(BadCredentialsException.class, () -> this.authenticationProvider.authenticate(token2));
        Mockito.verify(this.loginAttemptService, Mockito.times(2)).record(this.testData.VALID_USERNAME);

        final UsernamePasswordAuthenticationToken token3 = UsernamePasswordAuthenticationToken.unauthenticated(
                this.testData.VALID_USERNAME, "mismatch"
        );
        token3.setDetails(PrincipalType.USERNAME);
        Assertions.assertThrows(BadCredentialsException.class, () -> this.authenticationProvider.authenticate(token2));
        Mockito.verify(this.loginAttemptService, Mockito.times(3)).record(this.testData.VALID_USERNAME);
    }

    /**
     * 认证成功测试
     */
    @Test
    public void should_authenticated_and_set_details_when_success(){
        UsernamePasswordAuthenticationToken token = UsernamePasswordAuthenticationToken.unauthenticated(
                this.testData.VALID_USERNAME, this.testData.VALID_PASSWORD
        );
        token.setDetails(PrincipalType.USERNAME);
        Authentication res = this.authenticationProvider.authenticate(token);
        Assertions.assertTrue(res.isAuthenticated());
        Assertions.assertEquals(res.getDetails(), token.getDetails());
    }
}