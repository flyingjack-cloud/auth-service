package top.flyingjack.auth.account.service;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import top.flyingjack.common.tool.Verify;
import top.flyingjack.auth.account.entity.AuthUser;
import top.flyingjack.auth.account.service.repository.AuthUserRepository;

import java.util.Optional;

/**
 * Security用来获取User的Service
 *
 * @author Zumin Li
 * @date 2025/4/2 23:45
 */
@Service
public class UserDetailsServiceImpl implements LoginUserDetailService {
    private final AuthUserRepository authUserRepository;

    public UserDetailsServiceImpl(AuthUserRepository authUserRepository) {
        this.authUserRepository = authUserRepository;
    }

    /**
     * 根据username寻找User
     * @param username the username identifying the user whose data is required.
     * @return 实际上是AuthUser
     * @throws UsernameNotFoundException
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if(!Verify.verifyUsername(username)) {
            throw new UsernameNotFoundException("Invalid username");
        }

        Optional<AuthUser> authUser = this.authUserRepository.findAuthUserByUsername(username);
        return authUser.orElseThrow(() -> new UsernameNotFoundException(
                "User not found"
        ));
    }

    /**
     * 根据phone寻找User
     *
     * @param phone 会检查是否合法
     * @return 实际上是AuthUser
     * @throws UsernameNotFoundException
     */
    public UserDetails loadUserByPhone(String phone) throws UsernameNotFoundException {
        if(!Verify.verifyPhoneNumber(phone)) {
            throw new UsernameNotFoundException("Invalid phone number");
        }

        Optional<AuthUser> authUser = this.authUserRepository.findAuthUserByPhone(phone);
        return authUser.orElseThrow(() -> new UsernameNotFoundException(
                "User not found"
        ));
    }

    /**
     * 根据email寻找User
     *
     * @param email 会检查是否合法
     * @return 实际上是AuthUser
     * @throws UsernameNotFoundException
     */
    public UserDetails loadUserByEmail(String email) throws UsernameNotFoundException {
        if(!Verify.verifyEmail(email)) {
            throw new UsernameNotFoundException("Invalid email address");
        }

        Optional<AuthUser> authUser = this.authUserRepository.findAuthUserByEmail(email);
        return authUser.orElseThrow(() -> new UsernameNotFoundException(
                "User not found"
        ));
    }
}
