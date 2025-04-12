package top.flyingjack.auth.account.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import top.flyingjack.auth.CommonTestData;
import top.flyingjack.auth.account.entity.AuthUser;
import top.flyingjack.auth.account.entity.PrincipalType;
import top.flyingjack.auth.account.entity.dto.UserRequestDto;
import top.flyingjack.auth.account.service.repository.AuthUserRepository;
import top.flyingjack.auth.feign.client.CaptchaClient;
import top.flyingjack.common.error.exception.BusinessException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 注册相关服务测试，缓存在集合测试中测试
 *
 * @author Zumin Li
 * @date 2025/4/22 14:18
 */
class AccountServiceTest {
    private AuthUserRepository authUserRepository = Mockito.mock(AuthUserRepository.class);
    private CaptchaClient captchaClient = Mockito.mock(CaptchaClient.class);
    private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final CommonTestData testData = new CommonTestData();

    private AccountService service = new AccountService(
            authUserRepository,
            captchaClient,
            passwordEncoder
    );

    @BeforeEach
    void setUp() {
        Mockito.reset(authUserRepository);
        Mockito.reset(captchaClient);
    }

    @Test
    public void should_throw_error_with_invalid_paras_in_registration() {
        // 没有传入code
        UserRequestDto dto1 = new UserRequestDto(PrincipalType.PHONE, testData.VALID_PHONE,
                "justatest", "");
        assertThrows(IllegalArgumentException.class, () -> {this.service.register(dto1);});

        // 不支持的PrincipalType
        UserRequestDto dto2 = new UserRequestDto(PrincipalType.USERNAME, "justatest",
                "justatest", "123abc");
        assertThrows(IllegalArgumentException.class, () -> {this.service.register(dto2);});

        // 错误格式的principal
        UserRequestDto dto3 = new UserRequestDto(PrincipalType.PHONE, testData.INVALID_PHONE,
                "justatest", "123abc");
        assertThrows(IllegalArgumentException.class, () -> {this.service.register(dto3);});

        UserRequestDto dto4 = new UserRequestDto(PrincipalType.EMAIL, testData.INVALID_EMAIL,
                "justatest", "123abc");
        assertThrows(IllegalArgumentException.class, () -> {this.service.register(dto4);});
    }

    @Test
    public void should_throw_error_with_invalid_captcha_code_in_registration() {
        UserRequestDto dto1 = new UserRequestDto(PrincipalType.PHONE, testData.VALID_PHONE,
                "justatest", "codea1");
        assertThrows(BusinessException.class, () -> {this.service.register(dto1);});

        UserRequestDto dto2 = new UserRequestDto(PrincipalType.EMAIL, testData.VALID_EMAIL,
                "justatest", "codea1");
        assertThrows(BusinessException.class, () -> {this.service.register(dto2);});
    }

    @Test
    public void should_throw_error_with_existing_principal_in_registration() {
        Mockito.when(authUserRepository.existsAuthUserByPhone(testData.VALID_PHONE)).thenReturn(true);
        Mockito.when(authUserRepository.existsAuthUserByPhone(testData.VALID_EMAIL)).thenReturn(true);

        UserRequestDto dto1 = new UserRequestDto(PrincipalType.PHONE, testData.VALID_PHONE,
                "justatest", "codea1");
        assertThrows(BusinessException.class, () -> {this.service.register(dto1);});

        UserRequestDto dto2 = new UserRequestDto(PrincipalType.EMAIL, testData.VALID_EMAIL,
                "justatest", "codea1");
        assertThrows(BusinessException.class, () -> {this.service.register(dto2);});
    }

    @Test
    public void should_save_user_with_valid_paras_in_registration(){
        Mockito.when(this.captchaClient.verify(Mockito.any())).thenReturn(true);
        ArgumentCaptor<AuthUser> userCaptor = ArgumentCaptor.forClass(AuthUser.class);
        UserRequestDto dto1 = new UserRequestDto(PrincipalType.PHONE, testData.VALID_PHONE,
                "justatest", "codea1");
        this.service.register(dto1);
        Mockito.verify(authUserRepository).save(userCaptor.capture());
        assertNotNull(userCaptor.getValue());
        assertEquals(23, String.valueOf(userCaptor.getValue().getUsername()).length()); // 生成的username应该是一个user+雪花id

        Mockito.reset(authUserRepository);
        Mockito.when(this.captchaClient.verify(Mockito.any())).thenReturn(true);
        UserRequestDto dto2 = new UserRequestDto(PrincipalType.EMAIL, testData.VALID_EMAIL,
                "justatest", "codea1");
        this.service.register(dto2);
        Mockito.verify(authUserRepository).save(userCaptor.capture());
        assertNotNull(userCaptor.getValue());
        assertEquals(23, String.valueOf(userCaptor.getValue().getUsername()).length());
    }

    @Test
    public void should_throw_error_with_invalid_paras_in_reset() {
        // 没有传入code
        UserRequestDto dto1 = new UserRequestDto(PrincipalType.PHONE, testData.VALID_PHONE,
                testData.VALID_PASSWORD, "");
        assertThrows(IllegalArgumentException.class, () -> {this.service.resetPassword(dto1);});

        // 不支持的PrincipalType
        UserRequestDto dto2 = new UserRequestDto(PrincipalType.USERNAME, "justatest",
                "justatest", "123abc");
        assertThrows(IllegalArgumentException.class, () -> {this.service.resetPassword(dto2);});

        // 错误格式的principal
        UserRequestDto dto3 = new UserRequestDto(PrincipalType.PHONE, testData.INVALID_PHONE,
                testData.VALID_PASSWORD, "123abc");
        assertThrows(IllegalArgumentException.class, () -> {this.service.resetPassword(dto3);});

        UserRequestDto dto4 = new UserRequestDto(PrincipalType.EMAIL, testData.INVALID_EMAIL,
                testData.VALID_PASSWORD, "123abc");
        assertThrows(IllegalArgumentException.class, () -> {this.service.resetPassword(dto4);});
    }

    @Test
    public void should_throw_error_with_invalid_captcha_code_in_reset() {
        Mockito.when(authUserRepository.existsAuthUserByEmail(testData.VALID_EMAIL)).thenReturn(true);
        Mockito.when(authUserRepository.existsAuthUserByPhone(testData.VALID_PHONE)).thenReturn(true);

        UserRequestDto dto1 = new UserRequestDto(PrincipalType.PHONE, testData.VALID_PHONE,
                "justatest", "codea1");
        assertThrows(BusinessException.class, () -> {this.service.resetPassword(dto1);});

        UserRequestDto dto2 = new UserRequestDto(PrincipalType.EMAIL, testData.VALID_EMAIL,
                "justatest", "codea1");
        assertThrows(BusinessException.class, () -> {this.service.resetPassword(dto2);});
    }

    @Test
    public void should_throw_error_with_non_existing_principal_in_reset() {
        Mockito.when(authUserRepository.existsAuthUserByEmail(testData.VALID_EMAIL)).thenReturn(false);
        Mockito.when(authUserRepository.existsAuthUserByPhone(testData.VALID_PHONE)).thenReturn(false);

        UserRequestDto dto1 = new UserRequestDto(PrincipalType.PHONE, testData.VALID_PHONE,
                "justatest", "codea1");
        assertThrows(UsernameNotFoundException.class, () -> {this.service.resetPassword(dto1);});

        UserRequestDto dto2 = new UserRequestDto(PrincipalType.EMAIL, testData.VALID_EMAIL,
                "justatest", "codea1");
        assertThrows(UsernameNotFoundException.class, () -> {this.service.resetPassword(dto2);});
    }

    @Test
    public void should_update_password_with_valid_paras_in_reset(){
        Mockito.when(this.captchaClient.verify(Mockito.any())).thenReturn(true);
        Mockito.when(authUserRepository.existsAuthUserByEmail(testData.VALID_EMAIL)).thenReturn(true);
        Mockito.when(authUserRepository.existsAuthUserByPhone(testData.VALID_PHONE)).thenReturn(true);

        UserRequestDto dto1 = new UserRequestDto(PrincipalType.PHONE, testData.VALID_PHONE,
                testData.VALID_PASSWORD, "codea1");
        this.service.resetPassword(dto1);
        Mockito.verify(authUserRepository).updatePasswordByPhone(Mockito.any(), Mockito.eq(testData.VALID_PHONE));

        UserRequestDto dto2 = new UserRequestDto(PrincipalType.EMAIL, testData.VALID_EMAIL,
                testData.VALID_PASSWORD, "codea1");
        this.service.resetPassword(dto2);
        Mockito.verify(authUserRepository).updatePasswordByEmail(Mockito.any(), Mockito.eq(testData.VALID_EMAIL));
    }

    @Test
    public void should_throw_error_when_input_wrong_paras_into_exist_check() {
        assertThrows(IllegalArgumentException.class, () -> {
            this.service.isUsernameExist(testData.INVALID_USERNAME);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            this.service.isEmailExist(testData.INVALID_EMAIL);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            this.service.isPhoneExist(testData.INVALID_PHONE);
        });
    }

    @Test
    public void should_check_from_db_when_existence_check() {
        this.service.isUsernameExist(testData.VALID_USERNAME);
        Mockito.verify(this.authUserRepository, Mockito.times(1)).existsAuthUserByUsername(testData.VALID_USERNAME);

        this.service.isPhoneExist(testData.VALID_PHONE);
        Mockito.verify(this.authUserRepository, Mockito.times(1)).existsAuthUserByPhone(testData.VALID_PHONE);

        this.service.isEmailExist(testData.VALID_EMAIL);
        Mockito.verify(this.authUserRepository, Mockito.times(1)).existsAuthUserByEmail(testData.VALID_EMAIL);
    }
}