package top.flyingjack.auth.admin.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import top.flyingjack.auth.account.entity.AuthUser;
import top.flyingjack.auth.account.entity.Role;
import top.flyingjack.auth.account.service.repository.AuthUserRepository;
import top.flyingjack.auth.account.service.repository.RoleRepository;
import top.flyingjack.auth.admin.entity.dto.AdminUserDto;
import top.flyingjack.auth.admin.entity.dto.UpdateUserRolesDto;
import top.flyingjack.auth.admin.entity.dto.UpdateUserStatusDto;
import top.flyingjack.common.error.ErrorCode;
import top.flyingjack.common.error.exception.BusinessException;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminUserService {

    private final AuthUserRepository userRepository;
    private final RoleRepository roleRepository;

    public AdminUserService(AuthUserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    public Page<AdminUserDto> findAllUsers(Pageable pageable, String search) {
        Page<AuthUser> users = StringUtils.hasText(search)
                ? userRepository.findByUsernameContainingIgnoreCase(search, pageable)
                : userRepository.findAll(pageable);
        return users.map(this::toDto);
    }

    public AdminUserDto findUserById(Long id) {
        return userRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Transactional
    public void updateUserStatus(Long id, UpdateUserStatusDto dto) {
        AuthUser user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        if (dto.enabled() != null) user.setEnabled(dto.enabled());
        if (dto.accountNonLocked() != null) user.setAccountNonLocked(dto.accountNonLocked());
        userRepository.save(user);
    }

    @Transactional
    public void updateUserRoles(Long id, UpdateUserRolesDto dto) {
        AuthUser user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Set<Role> newRoles = new HashSet<>(roleRepository.findAllById(dto.roleIds()));
        user.setRoles(newRoles);
        userRepository.save(user);
    }

    @Transactional
    public void deleteUser(Long id) {
        AuthUser user = userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        // 先清空 user_role 关联，再删除用户（避免 FK 约束报错）
        user.setRoles(new HashSet<>());
        userRepository.save(user);
        userRepository.deleteById(id);
    }

    private AdminUserDto toDto(AuthUser user) {
        Set<String> roles = user.getRoles() == null ? Set.of()
                : user.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        return new AdminUserDto(
                user.getId(),
                user.getUsername(),
                user.getPhone(),
                user.getEmail(),
                user.getCreatedAt(),
                user.isEnabled(),
                user.isAccountNonLocked(),
                user.isAccountNonExpired(),
                user.isCredentialsNonExpired(),
                roles
        );
    }
}
