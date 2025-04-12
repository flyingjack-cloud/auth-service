package top.flyingjack.auth.account.controller;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import top.flyingjack.auth.CommonTestData;
import top.flyingjack.auth.account.entity.PrincipalType;
import top.flyingjack.auth.account.entity.dto.UserRequestDto;
import top.flyingjack.auth.feign.client.CaptchaClient;
import top.flyingjack.auth.integration.BaseContainerTest;
import top.flyingjack.common.cache.CacheService;
import top.flyingjack.common.dto.ApiRes;
import top.flyingjack.common.dto.UserDto;
import top.flyingjack.common.error.ErrorCode;
import top.flyingjack.common.error.ErrorRes;
import top.flyingjack.common.tool.HttpTools;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Zumin Li
 * @date 2025/4/22 16:06
 */
class RegisterControllerTest extends BaseContainerTest {
    private CommonTestData testData = new CommonTestData();

    @MockBean
    private CaptchaClient captchaClient;

    @Autowired
    private CacheService cacheService;

    @Autowired
    private TestRestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        Locale.setDefault(Locale.ENGLISH); // 消除jvm对测试结果的影响
    }

    @Test
    public void should_get_error_res_with_missing_para_in_exist_check() {
        ResponseEntity<ErrorRes> response = restTemplate.exchange(
                "/account/check/username",
                HttpMethod.GET,
                HttpTools.zhHeader(),
                ErrorRes.class
        );
        Assertions.assertEquals(ErrorCode.MISSING_REQUIRED_FIELD.getCode(), response.getBody().code());

        response = restTemplate.exchange(
                "/account/check/email",
                HttpMethod.GET,
                HttpTools.zhHeader(),
                ErrorRes.class
        );
        Assertions.assertEquals(ErrorCode.MISSING_REQUIRED_FIELD.getCode(), response.getBody().code());

        response = restTemplate.exchange(
                "/account/check/phone",
                HttpMethod.GET,
                HttpTools.zhHeader(),
                ErrorRes.class
        );
        Assertions.assertEquals(ErrorCode.MISSING_REQUIRED_FIELD.getCode(), response.getBody().code());
    }

    @Test
    public void should_get_error_res_with_invalid_para_in_exist_check() {
        ResponseEntity<ErrorRes> response = restTemplate.exchange(
                "/account/check/username?username={username}",
                HttpMethod.GET,
                HttpTools.zhHeader(),
                ErrorRes.class,
                this.testData.INVALID_USERNAME
        );
        Assertions.assertEquals(ErrorCode.INVALID_PARAM.getCode(), response.getBody().code());

        response = restTemplate.exchange(
                "/account/check/phone?phone={phone}",
                HttpMethod.GET,
                HttpTools.zhHeader(),
                ErrorRes.class,
                this.testData.INVALID_PHONE
        );
        Assertions.assertEquals(ErrorCode.INVALID_PARAM.getCode(), response.getBody().code());

        response = restTemplate.exchange(
                "/account/check/email?email={email}",
                HttpMethod.GET,
                HttpTools.zhHeader(),
                ErrorRes.class,
                this.testData.INVALID_EMAIL
        );
        Assertions.assertEquals(ErrorCode.INVALID_PARAM.getCode(), response.getBody().code());
    }

    @Test
    public void should_cache_result_with_valid_para_in_exist_check() {
        // 用户名
        ResponseEntity<ApiRes> response = restTemplate.exchange(
                "/account/check/username?username={username}",
                HttpMethod.GET,
                HttpTools.zhHeader(),
                ApiRes.class,
                this.testData.VALID_USERNAME
        );

        Assertions.assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
        // 检查成功后应当使用将结果存入缓存
        assertTrue(this.cacheService.hasKey("user::existCheck:" + this.testData.VALID_USERNAME));
        boolean resInCache = (Boolean) this.cacheService.get("user::existCheck:" + this.testData.VALID_USERNAME);
        assertEquals(resInCache, (Boolean) response.getBody().getData());

        // 手机号
        response = restTemplate.exchange(
                "/account/check/phone?phone={phone}",
                HttpMethod.GET,
                HttpTools.zhHeader(),
                ApiRes.class,
                this.testData.VALID_PHONE
        );

        Assertions.assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
        // 检查成功后应当使用将结果存入缓存
        assertTrue(this.cacheService.hasKey("user::existCheck:" + this.testData.VALID_PHONE));
        resInCache = (Boolean) this.cacheService.get("user::existCheck:" + this.testData.VALID_PHONE);
        assertEquals(resInCache, response.getBody().getData());


        // 邮件
        response = restTemplate.exchange(
                "/account/check/email?email={email}",
                HttpMethod.GET,
                HttpTools.zhHeader(),
                ApiRes.class,
                this.testData.VALID_EMAIL
        );

        Assertions.assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
        // 检查成功后应当使用将结果存入缓存
        assertTrue(this.cacheService.hasKey("user::existCheck:" + this.testData.VALID_EMAIL));
        resInCache = (Boolean) this.cacheService.get("user::existCheck:" + this.testData.VALID_EMAIL);
        assertEquals(resInCache, response.getBody().getData());
    }

    @Test
    public void should_get_error_with_invalid_info_in_registration(){
        // 1.缺乏请求体
        ResponseEntity<ErrorRes> response = restTemplate.postForEntity(
                "/account/register",
                null,
                ErrorRes.class
        );
        Assertions.assertEquals(ErrorCode.MISSING_REQUIRED_FIELD.getCode(), response.getBody().code());

        // 2.参数不合法
        UserRequestDto userRequestDto = new UserRequestDto(
                PrincipalType.PHONE,
                "",
                testData.VALID_PASSWORD,
                null
        );
        response = restTemplate.postForEntity(
                "/account/register",
                userRequestDto,
                ErrorRes.class
        );
        Assertions.assertEquals(ErrorCode.INVALID_PARAM.getCode(), response.getBody().code());

        userRequestDto = new UserRequestDto(
                PrincipalType.PHONE,
                testData.VALID_PHONE,
                "a",  // 密码格式不正确
                null
        );
        response = restTemplate.postForEntity(
                "/account/register",
                userRequestDto,
                ErrorRes.class
        );
        Assertions.assertEquals(ErrorCode.INVALID_PARAM.getCode(), response.getBody().code());
    }

    @Test
    public void should_get_error_with_existing_account_in_registration(){
        UserRequestDto userRequestDto = new UserRequestDto(
                PrincipalType.PHONE,
                testData.VALID_PHONE,
                testData.VALID_PASSWORD,
                "abc123"
        );
        ResponseEntity<ErrorRes> response = restTemplate.postForEntity(
                "/account/register",
                userRequestDto,
                ErrorRes.class
        );
        Assertions.assertEquals(ErrorCode.OBJECT_CONFLICT.getCode(), response.getBody().code());

        userRequestDto = new UserRequestDto(
                PrincipalType.EMAIL,
                testData.VALID_EMAIL,
                testData.VALID_PASSWORD, 
                "abc123"
        );
        response = restTemplate.postForEntity(
                "/account/register",
                userRequestDto,
                ErrorRes.class
        );
        Assertions.assertEquals(ErrorCode.OBJECT_CONFLICT.getCode(), response.getBody().code());
    }

    @Test
    public void should_get_user_dto_when_register_successfully(){
        Mockito.when(this.captchaClient.verify(Mockito.any())).thenReturn(true);
        UserRequestDto userRequestDto = new UserRequestDto(
                PrincipalType.PHONE,
                "15555555555",
                testData.VALID_PASSWORD,
                "abc123"
        );
        ResponseEntity<ApiRes<UserDto>> response = restTemplate.exchange(
                "/account/register",
                HttpMethod.POST,
                new HttpEntity<>(userRequestDto),
                new ParameterizedTypeReference<ApiRes<UserDto>>() {}
        );
        assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
        assertInstanceOf(UserDto.class, response.getBody().getData());
        UserDto userDto = response.getBody().getData();
        assertEquals(18, String.valueOf(userDto.id()).length());
        assertEquals("15555555555", userDto.phone());

        userRequestDto = new UserRequestDto(
                PrincipalType.EMAIL,
                "newusertest1@test.com",
                testData.VALID_PASSWORD,
                "abc123"
        );
        response = restTemplate.exchange(
                "/account/register",
                HttpMethod.POST,
                new HttpEntity<>(userRequestDto),
                new ParameterizedTypeReference<ApiRes<UserDto>>() {}
        );
        assertEquals(HttpStatusCode.valueOf(200), response.getStatusCode());
        assertInstanceOf(UserDto.class, response.getBody().getData());
        userDto = response.getBody().getData();
        assertEquals(18, String.valueOf(userDto.id()).length());
        assertEquals("newusertest1@test.com", userDto.email());
    }

    @Test
    public void should_clear_cache_when_calling_register(){
        Mockito.when(this.captchaClient.verify(Mockito.any())).thenReturn(true);

        String phone = "13032145789";
        restTemplate.exchange(
                "/account/check/phone?phone={phone}",
                HttpMethod.GET,
                HttpTools.zhHeader(),
                ApiRes.class,
                phone
        );
        assertTrue(this.cacheService.hasKey("user::existCheck:" + phone));

        UserRequestDto userRequestDto1 = new UserRequestDto(
                PrincipalType.PHONE,
                phone,
                testData.VALID_PASSWORD,
                "abc123"
        );
        restTemplate.exchange(
                "/account/register",
                HttpMethod.POST,
                new HttpEntity<>(userRequestDto1),
                new ParameterizedTypeReference<ApiRes<UserDto>>() {}
        );
        assertFalse(this.cacheService.hasKey("user::existCheck:" + phone));

        String email = "13032145789@test.com";
        restTemplate.exchange(
                "/account/check/email?email={email}",
                HttpMethod.GET,
                HttpTools.zhHeader(),
                ApiRes.class,
                email
        );
        assertTrue(this.cacheService.hasKey("user::existCheck:" + email));

        UserRequestDto userRequestDto2 = new UserRequestDto(
                PrincipalType.EMAIL,
                email,
                testData.VALID_PASSWORD,
                "abc123"
        );
        restTemplate.exchange(
                "/account/register",
                HttpMethod.POST,
                new HttpEntity<>(userRequestDto2),
                new ParameterizedTypeReference<ApiRes<UserDto>>() {}
        );
        assertFalse(this.cacheService.hasKey("user::existCheck:" + email));
    }
}