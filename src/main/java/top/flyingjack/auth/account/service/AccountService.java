package top.flyingjack.auth.account.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.flyingjack.auth.account.entity.AuthUser;
import top.flyingjack.auth.account.entity.PrincipalType;
import top.flyingjack.auth.account.entity.dto.ChangePasswordDto;
import top.flyingjack.auth.account.entity.dto.UpdateProfileDto;
import top.flyingjack.auth.account.entity.dto.UserRequestDto;
import top.flyingjack.auth.account.service.repository.AuthUserRepository;
import top.flyingjack.auth.config.SnowflakeIdGenerator;
import top.flyingjack.auth.feign.client.CaptchaClient;
import top.flyingjack.common.dto.CaptchaRequest;
import top.flyingjack.common.error.ErrorCode;
import top.flyingjack.common.error.exception.BusinessException;
import top.flyingjack.common.tool.SnowflakeIdGeneratorDelegate;
import top.flyingjack.common.tool.Verify;

import java.time.Instant;

/**
 * 注册相关服务
 *
 * @author Zumin Li
 * @date 2025/4/16 16:44
 */
@Service
public class AccountService {
    private static final Logger log = LoggerFactory.getLogger(AccountService.class);
    private final AuthUserRepository authUserRepository;
    private final CaptchaClient captchaClient;
    private final PasswordEncoder passwordEncoder;
    private final SnowflakeIdGeneratorDelegate snowflakeIdGenerator;
    private final CacheManager cacheManager;

    public AccountService(AuthUserRepository authUserRepository, CaptchaClient captchaClient,
                          PasswordEncoder passwordEncoder, SnowflakeIdGenerator snowflakeIdGenerator,
                          CacheManager cacheManager) {
        this.authUserRepository = authUserRepository;
        this.captchaClient = captchaClient;
        this.passwordEncoder = passwordEncoder;
        this.snowflakeIdGenerator = snowflakeIdGenerator.getDelegate();
        this.cacheManager = cacheManager;
    }

    /**
     * 注册账号 - 调用时会删除exist缓存
     *
     * @param userRequestDto 账号注册信息
     */
    @CacheEvict(value = "user", key = "'existCheck:' + #userRequestDto.principal()")
    public AuthUser register(UserRequestDto userRequestDto) {
        dtoPreCheck(userRequestDto);

        // 2.1 邮件注册流程
        if (userRequestDto.registerType() == PrincipalType.EMAIL) {
            if (isEmailExist(userRequestDto.principal())) {
                throw new BusinessException(ErrorCode.OBJECT_CONFLICT);
            }

            checkCaptcha(userRequestDto);

            AuthUser registeredUser = new AuthUser();
            // 移除特殊符号作为username
            registeredUser.setUsername(generateUsername());
            registeredUser.setEmail(userRequestDto.principal());
            registeredUser.setPassword(this.passwordEncoder.encode(userRequestDto.password()));
            registeredUser.setCreatedAt(Instant.now());
            return this.authUserRepository.save(registeredUser);
        }

        // 2.2 手机号注册流程
        else if (userRequestDto.registerType() == PrincipalType.PHONE) {
            if (isPhoneExist(userRequestDto.principal())) {
                throw new BusinessException(ErrorCode.OBJECT_CONFLICT);
            }

            checkCaptcha(userRequestDto);

            AuthUser registeredUser = new AuthUser();
            registeredUser.setUsername(generateUsername());
            registeredUser.setPhone(userRequestDto.principal());
            registeredUser.setPassword(this.passwordEncoder.encode(userRequestDto.password()));
            registeredUser.setCreatedAt(Instant.now());
            return this.authUserRepository.save(registeredUser);
        } else {
            throw new IllegalArgumentException("Invalid principal or type" + userRequestDto.registerType());
        }
    }

    /**
     * 重置密码
     *
     * @param userRequestDto 账号信息
     */
    public void resetPassword(UserRequestDto userRequestDto){
        dtoPreCheck(userRequestDto);
        String encryptedPassword = this.passwordEncoder.encode(userRequestDto.password());

        // 重置流程
        if (userRequestDto.registerType() == PrincipalType.EMAIL) {
            if (!isEmailExist(userRequestDto.principal())) {
                throw new UsernameNotFoundException("cannot found email to reset -" + userRequestDto.principal());
            }

            // 验证验证正确性
            checkCaptcha(userRequestDto);
            this.authUserRepository.updatePasswordByEmail(encryptedPassword, userRequestDto.principal());
        }
        else if (userRequestDto.registerType() == PrincipalType.PHONE) {
            if (!isPhoneExist(userRequestDto.principal())) {
                throw new UsernameNotFoundException("cannot found phone to reset -" + userRequestDto.principal());
            }

            checkCaptcha(userRequestDto);
            this.authUserRepository.updatePasswordByPhone(encryptedPassword, userRequestDto.principal());
        } else {
            throw new IllegalArgumentException("Invalid principal or type" + userRequestDto.registerType());
        }
    }

    /**
     * 检查username是否已经存在(带缓存)
     *
     * @param username 手机号
     */
    @Cacheable(value = "user", key = "'existCheck:' + #username")
    public boolean isUsernameExist(String username) {
        if (!Verify.verifyUsername(username)) {
            throw new IllegalArgumentException("Invalid username" + username);
        }

        return this.authUserRepository.existsAuthUserByUsername(username);
    }

    /**
     * 检查email是否已经存在(带缓存)
     *
     * @param email 邮箱地址
     */
    @Cacheable(value = "user", key = "'existCheck:' + #email")
    public boolean isEmailExist(String email) {
        if (!Verify.verifyEmail(email)) {
            throw new IllegalArgumentException("Invalid email address" + email);
        }

        return this.authUserRepository.existsAuthUserByEmail(email);
    }

    /**
     * 检查phone是否已经存在(带缓存)
     *
     * @param phone 手机号
     */
    @Cacheable(value = "user", key = "'existCheck:' + #phone")
    public boolean isPhoneExist(String phone) {
        if (!Verify.verifyPhoneNumber(phone)) {
            throw new IllegalArgumentException("Invalid phone number" + phone);
        }

        return this.authUserRepository.existsAuthUserByPhone(phone);
    }

    private static void dtoPreCheck(UserRequestDto userRequestDto) {
        // 1. 检查是否包含验证码和密码格式
        if (!StringUtils.hasLength(userRequestDto.code()) || !Verify.verifyPassword(userRequestDto.password())) {
            log.warn("Missing verify code or invalid password format for registerType={}", userRequestDto.registerType());
            throw new IllegalArgumentException("Invalid password or captcha" + userRequestDto.registerType());
        }
    }

    // 调用服务检查验证码是否正确
    private void checkCaptcha(UserRequestDto dto) {
        if (!this.captchaClient.verify(new CaptchaRequest(dto.principal(), dto.code()))) {
            throw new BusinessException(ErrorCode.NEED_CAPTCHA);
        }
    }

    /**
     * 获取当前登录用户信息
     */
    public AuthUser getProfile(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()
                || !(authentication.getPrincipal() instanceof AuthUser)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        AuthUser principal = (AuthUser) authentication.getPrincipal();
        // 从 DB 刷新以获取最新数据（session 中的 principal 可能已过期）
        return authUserRepository.findById(principal.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    /**
     * 修改个人资料（目前仅支持修改用户名）
     */
    @Transactional
    public AuthUser updateProfile(Authentication authentication, UpdateProfileDto dto) {
        AuthUser current = getProfile(authentication);

        if (!StringUtils.hasText(dto.username())) {
            return current;
        }

        String newUsername = dto.username().trim();
        if (!Verify.verifyUsername(newUsername)) {
            throw new IllegalArgumentException("Invalid username format: " + newUsername);
        }
        if (isUsernameExist(newUsername)) {
            throw new BusinessException(ErrorCode.OBJECT_CONFLICT);
        }

        // 清除新旧用户名的 exist 缓存
        var cache = cacheManager.getCache("user");
        if (cache != null) {
            cache.evict("existCheck:" + current.getUsername());
            cache.evict("existCheck:" + newUsername);
        }

        authUserRepository.updateUsernameById(newUsername, current.getId());
        return authUserRepository.findById(current.getId()).orElseThrow();
    }

    /**
     * 已登录状态下修改密码（需要旧密码验证）
     */
    @Transactional
    public void changePassword(Authentication authentication, ChangePasswordDto dto) {
        AuthUser current = getProfile(authentication);

        if (!passwordEncoder.matches(dto.oldPassword(), current.getPassword())) {
            throw new BusinessException(ErrorCode.WRONG_PASSWORD);
        }
        if (!Verify.verifyPassword(dto.newPassword())) {
            throw new IllegalArgumentException("Invalid new password format");
        }

        authUserRepository.updatePasswordById(passwordEncoder.encode(dto.newPassword()), current.getId());
    }

    // 生成一个雪花id用户名
    private String generateUsername() {
        return "user-" + this.snowflakeIdGenerator.nextId();
    }
}
