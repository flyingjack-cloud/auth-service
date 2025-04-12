package top.flyingjack.auth;

import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import top.flyingjack.auth.account.entity.AuthUser;
import top.flyingjack.auth.account.service.LoginUserDetailService;
import top.flyingjack.auth.account.service.UserDetailsServiceImpl;

/**
 * 常用的测试数据
 *
 * @author Zumin Li
 * @date 2025/4/6 0:20
 */
public class CommonTestData {
    public final String NOT_EXIST = "not_exist";
    public final String BLANK = "";

    public final String INVALID_USERNAME = "a!@";
    public final String INVALID_PHONE = "1023";
    public final String INVALID_EMAIL = "invalid@45";
    public final String NONE_PROVIDED = "NONE_PROVIDED";

    public final long VALID_ID = 303066491119603712L;
    public final String VALID_USERNAME = "testuser";
    public final String VALID_PHONE = "13012345678";
    public final String VALID_EMAIL = "test@test.com";
    public final UserDetails VALID_AUTH_USER;
    public final String VALID_PASSWORD = "abc123456789";
    public final String VALID_PASSWORD_ENCRYPTED;


    // 包含测试打桩的service，具体打桩数据请看resetUpLoginUserDetailService(), 使用后请务必调用resetUpLoginUserDetailService
    public final LoginUserDetailService LOGIN_USER_DETAIL_SERVICE = Mockito.mock(UserDetailsServiceImpl.class);

    public CommonTestData(){
        VALID_PASSWORD_ENCRYPTED = new BCryptPasswordEncoder().encode(VALID_PASSWORD);
        VALID_AUTH_USER = new AuthUser(VALID_USERNAME, VALID_PASSWORD_ENCRYPTED , VALID_PHONE, VALID_EMAIL);
        resetUpLoginUserDetailService();
    }

    public void resetUpLoginUserDetailService(){
        Mockito.reset(LOGIN_USER_DETAIL_SERVICE);

        Mockito.when(LOGIN_USER_DETAIL_SERVICE.loadUserByUsername(this.BLANK))
                .thenThrow(UsernameNotFoundException.class);
        Mockito.when(LOGIN_USER_DETAIL_SERVICE.loadUserByPhone(this.BLANK))
                .thenThrow(UsernameNotFoundException.class);
        Mockito.when(LOGIN_USER_DETAIL_SERVICE.loadUserByEmail(this.BLANK))
                .thenThrow(UsernameNotFoundException.class);

        Mockito.when(LOGIN_USER_DETAIL_SERVICE.loadUserByUsername(this.INVALID_USERNAME))
                .thenThrow(UsernameNotFoundException.class);
        Mockito.when(LOGIN_USER_DETAIL_SERVICE.loadUserByPhone(this.INVALID_PHONE))
                .thenThrow(UsernameNotFoundException.class);
        Mockito.when(LOGIN_USER_DETAIL_SERVICE.loadUserByEmail(this.INVALID_EMAIL))
                .thenThrow(UsernameNotFoundException.class);

        Mockito.doReturn(VALID_AUTH_USER).when(LOGIN_USER_DETAIL_SERVICE).loadUserByUsername(this.VALID_USERNAME);
        Mockito.doReturn(VALID_AUTH_USER).when(LOGIN_USER_DETAIL_SERVICE).loadUserByPhone(this.VALID_PHONE);
        Mockito.doReturn(VALID_AUTH_USER).when(LOGIN_USER_DETAIL_SERVICE).loadUserByEmail(this.VALID_EMAIL);
    }
}
