package top.flyingjack.auth.account.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import top.flyingjack.auth.account.entity.AuthUser;
import top.flyingjack.auth.account.entity.dto.UserRequestDto;
import top.flyingjack.auth.account.service.AccountService;
import top.flyingjack.common.dto.ApiRes;
import top.flyingjack.common.dto.UserDto;

/**
 * 用户注册管理
 *
 * @author Zumin Li
 * @date 2025/4/16 16:31
 */
@RestController
@RequestMapping("/account")
@Tag(name = "用户注册", description = "注册流程相关接口")
public class RegisterController {
    private AccountService accountService;

    public RegisterController(AccountService accountService) {
        this.accountService = accountService;
    }

    /**
     * 检测phone是否存在
     *
     * @param username 用户名
     * @return true 已存在
     *         false 不存在
     */
    @GetMapping("/check/username")
    @Operation(summary = "检查用户名是否已经注册")
    public ResponseEntity<ApiRes<Boolean>> usernameCheck(@RequestParam("username") String username) {
        return ResponseEntity.ok().body(ApiRes.success(
                this.accountService.isUsernameExist(username)
        ));
    }

    /**
     * 检测email是否存在
     *
     * @param email 邮箱地址
     * @return true 已存在
     *         false 不存在
     */
    @GetMapping("/check/email")
    @Operation(summary = "检查邮箱地址是否已经注册")
    public ResponseEntity<ApiRes<Boolean>> emailCheck(@RequestParam("email") String email) {
        return ResponseEntity.ok().body(ApiRes.success(
                this.accountService.isEmailExist(email)
        ));
    }

    /**
     * 检测phone是否存在
     *
     * @param phone 手机号
     * @return true 已存在
     *         false 不存在
     */
    @GetMapping("/check/phone")
    @Operation(summary = "检查手机号是否已经注册")
    public ResponseEntity<ApiRes<Boolean>> phoneCheck(@RequestParam("phone") String phone) {
        return ResponseEntity.ok().body(ApiRes.success(
                this.accountService.isPhoneExist(phone)
        ));
    }

    @PostMapping("/register")
    @Operation(summary = "注册")
    public ResponseEntity<ApiRes<UserDto>> register(@RequestBody UserRequestDto userRequestDto) {
        // 不允许直接返回用户，需要进行投影
        AuthUser user = this.accountService.register(userRequestDto);

        return ResponseEntity.ok().body(ApiRes.success(
            new UserDto(user.getId(), user.getUsername(), user.getPhone(), user.getEmail())
        ));
    }
}
