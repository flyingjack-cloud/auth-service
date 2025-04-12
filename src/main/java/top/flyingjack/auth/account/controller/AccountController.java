package top.flyingjack.auth.account.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.flyingjack.auth.account.entity.AuthUser;
import top.flyingjack.auth.account.entity.dto.UserRequestDto;
import top.flyingjack.auth.account.service.AccountService;
import top.flyingjack.common.dto.ApiRes;
import top.flyingjack.common.dto.UserDto;

/**
 * @author Zumin Li
 * @date 2025/4/22 22:43
 */
@RestController
@RequestMapping("/account")
@Tag(name = "其他用户相关管理接口", description = "包含密码重置")
public class AccountController {
    private AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping("/reset-password")
    @Operation(summary = "重置密码")
    public ResponseEntity<ApiRes<?>> register(@RequestBody UserRequestDto userRequestDto) {
        this.accountService.resetPassword(userRequestDto);
        return ResponseEntity.ok().body(ApiRes.success());
    }
}
