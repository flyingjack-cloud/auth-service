package top.flyingjack.auth.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import top.flyingjack.auth.account.entity.AuthUser;
import top.flyingjack.auth.account.service.UserDetailsServiceImpl;
import top.flyingjack.auth.account.service.repository.AuthUserRepository;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

/**
 * UserDetailsService测试 - 只测试Security相关的
 *
 * @author Zumin Li
 * @date 2025/4/3 1:34
 */
class UserDetailsServiceImplTest {
    private UserDetailsServiceImpl userService;
    private AuthUserRepository authUserRepository;
    private AuthUser user;

    public UserDetailsServiceImplTest() {
        authUserRepository = Mockito.mock(AuthUserRepository.class);
        userService = new UserDetailsServiceImpl(authUserRepository);
        // 数据不存在统一打桩
        when(authUserRepository.findAuthUserByUsername("not_exist")).thenReturn(Optional.empty());
        when(authUserRepository.findAuthUserByPhone("13000000000")).thenReturn(Optional.empty());
        when(authUserRepository.findAuthUserByEmail("not_exist@text.com")).thenReturn(Optional.empty());

        // 数据正常统一打桩
        user = new AuthUser("username","abc123456" ,"13012345678","test@test.com");
        when(authUserRepository.findAuthUserByUsername("username")).thenReturn(Optional.of(user));
        when(authUserRepository.findAuthUserByPhone("13012345678")).thenReturn(Optional.of(user));
        when(authUserRepository.findAuthUserByEmail("test@test.com")).thenReturn(Optional.of(user));
    }

    /**
     * 参数不合法或不存在时测试
     *
     */
    @Test
    public void should_throw_authentication_exception_when_username_is_blank_or_not_in_db() {
        Assertions.assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername(null));
        Assertions.assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername(""));
        Assertions.assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByUsername("not_exist"));

        Assertions.assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByPhone(null));
        Assertions.assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByPhone(""));
        Assertions.assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByPhone("13000000000"));

        Assertions.assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByEmail(null));
        Assertions.assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByEmail(""));
        Assertions.assertThrows(UsernameNotFoundException.class, () -> userService.loadUserByEmail("not_exist@text.com"));
    }

    /**
     * user存在时测试
     *
     */
    @Test
    public void should_get_user_when_username_exist() {
        assertThat(userService.loadUserByUsername("username").getUsername(), equalTo("username") );
        assertThat(userService.loadUserByPhone("13012345678"), notNullValue());
        assertThat(userService.loadUserByEmail("test@test.com"), notNullValue());
    }
}