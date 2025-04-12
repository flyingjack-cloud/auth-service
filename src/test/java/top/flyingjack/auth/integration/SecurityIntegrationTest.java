package top.flyingjack.auth.integration;

import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import top.flyingjack.auth.CommonTestData;
import top.flyingjack.auth.account.entity.PrincipalType;
import top.flyingjack.auth.account.entity.dto.UserLoginDto;
import top.flyingjack.auth.feign.client.CaptchaClient;
import top.flyingjack.auth.account.filter.RestAuthenticationFilter;
import top.flyingjack.auth.account.service.LoginAttemptService;
import top.flyingjack.common.dto.ApiRes;
import top.flyingjack.common.dto.UserDto;
import top.flyingjack.common.error.ErrorCode;
import top.flyingjack.common.error.ErrorRes;
import top.flyingjack.common.tool.HttpTools;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Spring security认证相关的集合测试
 *
 * @author Zumin Li
 * @date 2025/4/11 16:52
 */
@ActiveProfiles("test")
public class SecurityIntegrationTest extends BaseContainerTest {
    @MockBean
    private CaptchaClient captchaClient;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @Autowired
    private TestRestTemplate restTemplate;

    private CommonTestData testData = new CommonTestData();

    @BeforeEach
    void setUp() {
        Locale.setDefault(Locale.ENGLISH); // 消除jvm对测试结果的影响
    }

    @AfterEach
    void clear(){
        this.loginAttemptService.clear(testData.VALID_USERNAME);
        this.loginAttemptService.clear(testData.VALID_PHONE);
        this.loginAttemptService.clear(testData.VALID_EMAIL);
    }

    /**
     * 未认证时访问受限端口 - InsufficientAuthenticationException
     */
    @Test
    public void should_get_insufficient_error_without_user() {
        ResponseEntity<ErrorRes> zhResponse = restTemplate.exchange(
                "/admin",
                HttpMethod.GET,
                HttpTools.zhHeader(),
                ErrorRes.class
        );
        Assertions.assertEquals(ErrorCode.INSUFFICIENT.getCode(), zhResponse.getBody().code());
        Assertions.assertEquals("认证信息不足", zhResponse.getBody().message());

        ResponseEntity<ErrorRes> enResponse = restTemplate.exchange(
                "/admin",
                HttpMethod.GET,
                HttpTools.usHeader(),
                ErrorRes.class
        );
        Assertions.assertEquals(ErrorCode.INSUFFICIENT.getCode(), enResponse.getBody().code());
        Assertions.assertEquals("Unauthorized", enResponse.getBody().message());
    }

    /**
     * 请求参数有误时登录过程测试
     */
    @Test
    public void should_get_relevant_error_with_invalid_login() throws JsonProcessingException {
        // 1. 请求method不对
        ResponseEntity<ErrorRes> zhMethodResponse = restTemplate.getForEntity("/account/login", ErrorRes.class);
        Assertions.assertEquals(ErrorCode.INSUFFICIENT.getCode(), zhMethodResponse.getBody().code());
        Assertions.assertEquals("Unauthorized", zhMethodResponse.getBody().message());

        // 2. 请求LoginRequest参数不存在
        ResponseEntity<ErrorRes> postResponse = restTemplate.postForEntity("/account/login", null, ErrorRes.class);
        Assertions.assertEquals(ErrorCode.INSUFFICIENT.getCode(), postResponse.getBody().code());
        Assertions.assertEquals("Unauthorized", postResponse.getBody().message());

        // 3. 请求LoginRequest缺少参数
        ResponseEntity<ErrorRes> paraResponse = restTemplate.postForEntity("/account/login", jsonRequest(Map.of(
                "principal", "testuser")), ErrorRes.class);
        Assertions.assertEquals(ErrorCode.INSUFFICIENT.getCode(), paraResponse.getBody().code());
        Assertions.assertEquals("Unauthorized", paraResponse.getBody().message());

        // 3. 请求LoginRequest json格式不正确
        ResponseEntity<ErrorRes> para2Response = restTemplate.postForEntity("/account/login", jsonRequest(Map.of(
                "what", "testuser")), ErrorRes.class);
        Assertions.assertEquals(ErrorCode.INSUFFICIENT.getCode(), para2Response.getBody().code());
        Assertions.assertEquals("Unauthorized", para2Response.getBody().message());

        // 4. 请求LoginRequest principal格式不正确
        UserLoginDto loginInvalidUsernameRequest = new UserLoginDto(PrincipalType.USERNAME,
                testData.INVALID_USERNAME,
                testData.VALID_PASSWORD, null);
        ResponseEntity<ErrorRes> para3Response = restTemplate.postForEntity("/account/login", loginInvalidUsernameRequest,
                ErrorRes.class);
        Assertions.assertEquals(ErrorCode.BAD_CREDENTIAL.getCode(), para3Response.getBody().code());
        Assertions.assertEquals("Bad authentication information", para3Response.getBody().message());

        UserLoginDto loginInvalidPhoneRequest = new UserLoginDto(PrincipalType.PHONE,
                testData.INVALID_PHONE,
                testData.VALID_PASSWORD, null);
        ResponseEntity<ErrorRes> para4Response = restTemplate.postForEntity("/account/login", loginInvalidPhoneRequest,
                ErrorRes.class);
        Assertions.assertEquals(ErrorCode.BAD_CREDENTIAL.getCode(), para4Response.getBody().code());
        Assertions.assertEquals("Bad authentication information", para4Response.getBody().message());

        UserLoginDto loginInvalidEmailRequest = new UserLoginDto(PrincipalType.EMAIL,
                testData.INVALID_EMAIL,
                testData.VALID_PASSWORD, null);
        ResponseEntity<ErrorRes> para5Response = restTemplate.postForEntity("/account/login", loginInvalidEmailRequest,
                ErrorRes.class);
        Assertions.assertEquals(ErrorCode.BAD_CREDENTIAL.getCode(), para5Response.getBody().code());
        Assertions.assertEquals("Bad authentication information", para5Response.getBody().message());
    }

    /**
     * 一般登录失败测试
     */
    @Test
    public void should_get_relevant_error_with_incompatible_info() {
        UserLoginDto loginUsernameRequest = new UserLoginDto(PrincipalType.USERNAME,
                testData.VALID_USERNAME,
                "notapassword", null);

        ResponseEntity<ErrorRes> response = restTemplate.postForEntity("/account/login", loginUsernameRequest,
                ErrorRes.class);
        Assertions.assertEquals(ErrorCode.BAD_CREDENTIAL.getCode(), response.getBody().code());
        Assertions.assertEquals("Bad authentication information", response.getBody().message());

        UserLoginDto loginPhoneRequest = new UserLoginDto(PrincipalType.PHONE,
                testData.VALID_USERNAME,
                "notapassword", null);
        ResponseEntity<ErrorRes> phoneResponse = restTemplate.postForEntity("/account/login", loginPhoneRequest,
                ErrorRes.class);
        Assertions.assertEquals(ErrorCode.BAD_CREDENTIAL.getCode(), phoneResponse.getBody().code());
        Assertions.assertEquals("Bad authentication information", phoneResponse.getBody().message());

        UserLoginDto loginEmailRequest = new UserLoginDto(PrincipalType.EMAIL,
                testData.VALID_EMAIL,
                "notapassword", null);

        ResponseEntity<ErrorRes> emailResponse = restTemplate.postForEntity("/account/login", loginEmailRequest,
                ErrorRes.class);
        Assertions.assertEquals(ErrorCode.BAD_CREDENTIAL.getCode(), emailResponse.getBody().code());
        Assertions.assertEquals("Bad authentication information", emailResponse.getBody().message());
    }

    /**
     * 多次登录失败测试
     */
    @Test
    public void should_get_error_with_too_many_login() {
        this.loginAttemptService.clear(testData.VALID_USERNAME); // 先清除状态，方便后续测试

        UserLoginDto loginUsernameRequest = new UserLoginDto(PrincipalType.USERNAME,
                testData.VALID_USERNAME,
                testData.INVALID_EMAIL, null);

        int i = 0;
        // 前三次普通错误返回
        while (i < 3){
            ResponseEntity<ErrorRes> res = restTemplate.postForEntity("/account/login", loginUsernameRequest, ErrorRes.class);
            i++;

            Assertions.assertEquals(i, this.loginAttemptService.count(testData.VALID_USERNAME));
            Assertions.assertEquals(ErrorCode.BAD_CREDENTIAL.getCode(), res.getBody().code());
            Assertions.assertEquals("Bad authentication information", res.getBody().message());
        }

        // 【3-10)次提示需要验证码
        while (i < 10){
            ResponseEntity<ErrorRes> res = restTemplate.postForEntity("/account/login", loginUsernameRequest, ErrorRes.class);
            i++;

            Assertions.assertEquals(i, this.loginAttemptService.count(testData.VALID_USERNAME));
            Assertions.assertEquals(ErrorCode.NEED_CAPTCHA.getCode(), res.getBody().code());
            Assertions.assertEquals("Missing valid captcha", res.getBody().message());
        }

        // 10次以上提示需要验证码
        while (i < 12){
            ResponseEntity<ErrorRes> res = restTemplate.postForEntity("/account/login", loginUsernameRequest, ErrorRes.class);
            i++;

            Assertions.assertEquals(10, this.loginAttemptService.count(testData.VALID_USERNAME));
            Assertions.assertEquals(ErrorCode.TO_MANY_LOGIN_ATTEMPT.getCode(), res.getBody().code());
            Assertions.assertEquals("To many attempt, wait 5 minutes", res.getBody().message());
        }
    }

    /**
     * 验证码输入测试
     */
    @Test
    public void should_login_or_error_with_relevant_captcha() {
        // 先清除状态，假设已经失败了三次
        this.loginAttemptService.clear(testData.VALID_USERNAME);
        this.loginAttemptService.record(testData.VALID_USERNAME);
        this.loginAttemptService.record(testData.VALID_USERNAME);
        this.loginAttemptService.record(testData.VALID_USERNAME);

        // 验证码错误测试
        UserLoginDto loginUsernameRequest = new UserLoginDto(PrincipalType.USERNAME,
                testData.VALID_USERNAME,
                testData.VALID_PASSWORD, null);
        HttpHeaders headers = new HttpHeaders();
        headers.set(RestAuthenticationFilter.CAPTCHA_ID_HEADER, UUID.randomUUID().toString());
        headers.set(RestAuthenticationFilter.CAPTCHA_TOKEN_HEADER, "ass123");
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<UserLoginDto> entity = new HttpEntity<>(loginUsernameRequest, headers);

        ResponseEntity<ErrorRes> res = restTemplate.exchange(
                "/account/login",
                HttpMethod.POST,
                entity,
                ErrorRes.class
        );
        Assertions.assertEquals(ErrorCode.NEED_CAPTCHA.getCode(), res.getBody().code());
        Assertions.assertEquals("Missing valid captcha", res.getBody().message());


        // 验证码正确测试
        Mockito.when(this.captchaClient.verify(Mockito.any())).thenReturn(true);
        ResponseEntity<ApiRes> res2 = restTemplate.exchange(
                "/account/login",
                HttpMethod.POST,
                entity,
                ApiRes.class
        );
        Assertions.assertEquals(HttpStatusCode.valueOf(200), res2.getStatusCode());
        Assertions.assertEquals("Success", res2.getBody().getMessage());
    }

    /**
     * 登陆成功测试
     */
    @Test
    public void should_login_with_compatible_user_info() {
        UserLoginDto loginUsernameRequest = new UserLoginDto(PrincipalType.USERNAME,
                testData.VALID_USERNAME,
                testData.VALID_PASSWORD, null);
        ResponseEntity<ApiRes<UserDto>> userResponse = restTemplate.exchange(
                "/account/login",
                HttpMethod.POST,
                new HttpEntity<>(loginUsernameRequest),
                new ParameterizedTypeReference<ApiRes<UserDto>>() {}
        );
        Assertions.assertEquals(HttpStatusCode.valueOf(200), userResponse.getStatusCode());
        Assertions.assertEquals("Success", userResponse.getBody().getMessage());
        Assertions.assertEquals(testData.VALID_ID,  userResponse.getBody().getData().id());
        Assertions.assertEquals(testData.VALID_USERNAME,  userResponse.getBody().getData().username());

        UserLoginDto loginPhoneRequest = new UserLoginDto(PrincipalType.PHONE,
                testData.VALID_PHONE,
                testData.VALID_PASSWORD, null);
        ResponseEntity<ApiRes> phoneResponse = restTemplate.postForEntity("/account/login", loginPhoneRequest,
                ApiRes.class);
        Assertions.assertEquals(HttpStatusCode.valueOf(200), phoneResponse.getStatusCode());
        Assertions.assertEquals("Success", phoneResponse.getBody().getMessage());

        UserLoginDto loginEmailRequest = new UserLoginDto(PrincipalType.EMAIL,
                testData.VALID_EMAIL,
                testData.VALID_PASSWORD, null);
        ResponseEntity<ApiRes> emailResponse = restTemplate.postForEntity("/account/login", loginEmailRequest,
                ApiRes.class);
        Assertions.assertEquals(HttpStatusCode.valueOf(200), emailResponse.getStatusCode());
        Assertions.assertEquals("Success", emailResponse.getBody().getMessage());
    }


    // 根据map生成请求体
    private HttpEntity<String> jsonRequest(Map<String, Object> entries) throws JsonProcessingException {
        String jsonString = new ObjectMapper().writeValueAsString(entries);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return new HttpEntity<>(jsonString, headers);
    }
}