package top.flyingjack.auth.account.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import top.flyingjack.auth.account.entity.PrincipalType;

/**
 * 用户注册和重置密码时请求
 *
 * @author Zumin Li
 * @date 2025/4/16 16:37
 */
@Schema(description = "用户注册请求")
public record UserRequestDto(
    @NotBlank
    @Schema(description = "登录类型", example = "phone")
    PrincipalType registerType,

    @NotBlank
    @Schema(description = "身份信息，根据Type变化", example = "13012345678")
    String principal,

    @NotBlank
    @Schema(description = "密码", example = "a23c123azsq")
    String password,

    @NotBlank
    @Schema(description = "邮箱或者手机验证码", example = "a23c123azsq")
    String code
) { }
