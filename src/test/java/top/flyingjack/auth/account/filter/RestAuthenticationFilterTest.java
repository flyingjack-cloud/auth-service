package top.flyingjack.auth.account.filter;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import top.flyingjack.auth.feign.client.CaptchaClient;
import top.flyingjack.auth.account.service.LoginAttemptService;
import top.flyingjack.common.error.exception.AttemptAuthenticationException;
import top.flyingjack.common.error.exception.CaptchaAuthenticationException;

import java.util.UUID;

/**
 * RestAuthenticationFilter测试 - 不测试父类功能
 *
 * @author Zumin Li
 * @date 2025/4/11 14:53
 */
class RestAuthenticationFilterTest {
    private LoginAttemptService loginAttemptService = Mockito.mock(LoginAttemptService.class);
    private CaptchaClient captchaClient = Mockito.mock(CaptchaClient.class);
    private AuthenticationManager manager = Mockito.mock(AuthenticationManager.class);

    private RestAuthenticationFilter restAuthenticationFilter;

    @BeforeEach
    void setUp(){
        this.restAuthenticationFilter = new RestAuthenticationFilter();
        this.restAuthenticationFilter.setLoginAttemptService(loginAttemptService);
        this.restAuthenticationFilter.setCaptchaClient(captchaClient);
        this.restAuthenticationFilter.setAuthenticationManager(manager);
    }

    @AfterEach
    void setDown(){
        Mockito.reset(this.loginAttemptService);
        Mockito.reset(this.captchaClient);
        Mockito.reset(this.manager);
    }

    @Test
    public void should_throw_exception_when_method_not_support(){
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("GET");

        Assertions.assertThrows(InsufficientAuthenticationException.class, () -> this.restAuthenticationFilter.attemptAuthentication(
                request, Mockito.mock(HttpServletResponse.class)
        ));
    }

    @Test
    public void should_throw_exception_when_get_invalid_body(){
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setContentType("application/json");
        request.setContent("{\"name\":\"test\"}".getBytes());

        Assertions.assertThrows(InsufficientAuthenticationException.class, () -> this.restAuthenticationFilter.attemptAuthentication(
                request, Mockito.mock(HttpServletResponse.class)
        ));
    }

    @Test
    public void should_throw_exception_when_attempt_too_many(){
        MockHttpServletRequest request = mockHttpServletRequest();
        Mockito.when(this.loginAttemptService.count("testuser")).thenReturn(10);

        Assertions.assertThrows(AttemptAuthenticationException.class, () -> this.restAuthenticationFilter.attemptAuthentication(
                request, Mockito.mock(HttpServletResponse.class)
        ));
        Mockito.verify(this.loginAttemptService, Mockito.never()).record("testuser"); // 锁定的账户，不再记录和刷新时间
    }

    @Test
    public void should_throw_exception_without_captcha_headers(){
        MockHttpServletRequest request = mockHttpServletRequest();
        Mockito.when(this.loginAttemptService.count("testuser")).thenReturn(4);

        Assertions.assertThrows(CaptchaAuthenticationException.class, () -> this.restAuthenticationFilter.attemptAuthentication(
                request, Mockito.mock(HttpServletResponse.class)
        ));
        Mockito.verify(this.loginAttemptService).record("testuser"); // 失败时应当记录

        MockHttpServletRequest request2 = mockHttpServletRequest();
        request2.addHeader("X-Captcha-Token", "asxc1");
        Assertions.assertThrows(CaptchaAuthenticationException.class, () -> this.restAuthenticationFilter.attemptAuthentication(
                request2, Mockito.mock(HttpServletResponse.class)
        ));

        MockHttpServletRequest request3 = mockHttpServletRequest();
        request3.addHeader("X-Captcha-Token", "asxc1");
        request3.addHeader("X-Captcha-ID", "notauuid");
        Assertions.assertThrows(CaptchaAuthenticationException.class, () -> this.restAuthenticationFilter.attemptAuthentication(
                request3, Mockito.mock(HttpServletResponse.class)
        ));
    }

    @Test
    public void should_throw_exception_with_invalid_uuid(){
        MockHttpServletRequest request = mockHttpServletRequest();
        request.addHeader("X-Captcha-ID", "notauuid");
        request.addHeader("X-Captcha-Token", "asxc1");
        Mockito.when(this.loginAttemptService.count("testuser")).thenReturn(4);

        Assertions.assertThrows(CaptchaAuthenticationException.class, () -> this.restAuthenticationFilter.attemptAuthentication(
                request, Mockito.mock(HttpServletResponse.class)
        ));
        Mockito.verify(this.loginAttemptService, Mockito.times(1)).record("testuser"); // 失败时应当记录
    }

    @Test
    public void should_throw_exception_when_verifying_failed(){
        MockHttpServletRequest request = mockHttpServletRequest();
        UUID uuid = UUID.randomUUID();
        request.addHeader("X-Captcha-ID", uuid.toString());
        request.addHeader("X-Captcha-Token", "asxc1");
        Mockito.when(this.loginAttemptService.count("testuser")).thenReturn(4);
        Mockito.when(this.captchaClient.verify(Mockito.any())).thenReturn(false);

        Assertions.assertThrows(CaptchaAuthenticationException.class, () -> this.restAuthenticationFilter.attemptAuthentication(
                request, Mockito.mock(HttpServletResponse.class)
        ));
        Mockito.verify(this.captchaClient, Mockito.times(1)).verify(Mockito.any());
    }

    @Test
    public void should_skip_captcha_check_when_attempt_to_less() {
        MockHttpServletRequest request = mockHttpServletRequest();
        Mockito.when(this.loginAttemptService.count("testuser")).thenReturn(2);

        this.restAuthenticationFilter.attemptAuthentication(request, Mockito.mock(HttpServletResponse.class));

        Mockito.verify(this.captchaClient, Mockito.never()).verify(Mockito.any());
        Mockito.verify(this.loginAttemptService, Mockito.never()).clear("testuser");
        Mockito.verify(this.manager, Mockito.times(1)).authenticate(Mockito.any());
    }

    @Test
    public void should_clear_record_when_verifying_successfully(){
        MockHttpServletRequest request = mockHttpServletRequest();
        request.addHeader("X-Captcha-ID", UUID.randomUUID().toString());
        request.addHeader("X-Captcha-Token", "asxc1");
        Mockito.when(this.loginAttemptService.count("testuser")).thenReturn(4);
        Mockito.when(this.captchaClient.verify(Mockito.any())).thenReturn(true);

        this.restAuthenticationFilter.attemptAuthentication(request, Mockito.mock(HttpServletResponse.class));

        Mockito.verify(this.captchaClient, Mockito.times(1)).verify(Mockito.any());
        Mockito.verify(this.loginAttemptService, Mockito.times(1)).clear("testuser");
        Mockito.verify(this.manager, Mockito.times(1)).authenticate(Mockito.any());
    }

    private MockHttpServletRequest mockHttpServletRequest(){
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setMethod("POST");
        request.setContentType("application/json");
        String jsonString = """
                        {
                          "loginType": "username",
                          "principal": "testuser",
                          "password": "123456789"
                        }
                        """;
        request.setContent(jsonString.getBytes());

        return request;
    }
}