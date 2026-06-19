package top.flyingjack.auth.account.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import top.flyingjack.auth.account.entity.AuthUser;
import top.flyingjack.auth.account.entity.dto.ChangePasswordDto;
import top.flyingjack.auth.account.entity.dto.UpdateProfileDto;
import top.flyingjack.auth.account.entity.dto.UserRequestDto;
import top.flyingjack.auth.account.service.AccountService;
import top.flyingjack.common.dto.ApiRes;
import top.flyingjack.common.dto.UserDto;

@RestController
@RequestMapping("/account")
@Tag(name = "账号管理", description = "密码重置、个人资料、密码修改")
public class AccountController {
    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/reset-password")
    @Operation(summary = "重置密码（未登录，需验证码）")
    public ResponseEntity<ApiRes<?>> resetPassword(@RequestBody UserRequestDto userRequestDto) {
        this.accountService.resetPassword(userRequestDto);
        return ResponseEntity.ok().body(ApiRes.success());
    }

    @GetMapping("/profile")
    @Operation(summary = "获取当前登录用户的个人资料")
    public ResponseEntity<ApiRes<UserDto>> getProfile(Authentication authentication) {
        AuthUser user = accountService.getProfile(authentication);
        return ResponseEntity.ok(ApiRes.success(
                new UserDto(user.getId(), user.getUsername(), user.getPhone(), user.getEmail())));
    }

    @PutMapping("/profile")
    @Operation(summary = "更新个人资料（目前支持修改用户名）")
    public ResponseEntity<ApiRes<UserDto>> updateProfile(
            Authentication authentication,
            @RequestBody UpdateProfileDto dto) {
        AuthUser updated = accountService.updateProfile(authentication, dto);
        return ResponseEntity.ok(ApiRes.success(
                new UserDto(updated.getId(), updated.getUsername(), updated.getPhone(), updated.getEmail())));
    }

    @PostMapping("/change-password")
    @Operation(summary = "修改密码（已登录，需提供旧密码）")
    public ResponseEntity<ApiRes<Void>> changePassword(
            Authentication authentication,
            @RequestBody ChangePasswordDto dto) {
        accountService.changePassword(authentication, dto);
        return ResponseEntity.ok(ApiRes.success());
    }
}
