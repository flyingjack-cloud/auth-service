package top.flyingjack.auth.account.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import top.flyingjack.common.dto.ApiRes;
import top.flyingjack.common.dto.UserDto;
import top.flyingjack.auth.account.entity.dto.UserLoginDto;

@RestController
@RequestMapping("/account")
@Tag(name = "用户登录", description = "包含登录等流程的用户管理接口")
public class LoginController {
    /**
     * 登录入口
     * - 该接口实际不会被调用，只是作为统一展示和doc用，删去不影响逻辑
     * - 具体实逻辑实现请看：
     *      入参 - RestAuthenticationFilter，AuthenticationManager
     *      成功返回 - LoginAuthenticationSuccessHandler
     *
     */
    @PostMapping("/login")
    @Operation(summary = "登录")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "401", description = "登录失败", content = @Content),
    })
    public ApiRes<UserDto> login(@Parameter(description = "登录参数", required = true) @RequestBody UserLoginDto userLoginDto) {
        // 认证逻辑将在Filter处理, 为了保证Oauth2的filter能在SecurityContext中直接获取Authentication
        return ApiRes.success();
    }
}
