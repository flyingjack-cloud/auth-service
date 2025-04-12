package top.flyingjack.auth.account.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * 支持多种登录方式的UserDetailsService
 *
 * @author Zumin Li
 * @date 2025/4/3 16:55
 */
public interface LoginUserDetailService extends UserDetailsService {
    UserDetails loadUserByPhone(String phone) throws UsernameNotFoundException;
    UserDetails loadUserByEmail(String email) throws UsernameNotFoundException;
}