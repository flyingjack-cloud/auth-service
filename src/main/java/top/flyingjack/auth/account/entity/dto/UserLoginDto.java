package top.flyingjack.auth.account.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotBlank;
import top.flyingjack.auth.account.entity.PrincipalType;

@Schema(description = "用户登录请求")
public record UserLoginDto(
    @NotBlank
    @Schema(description = "登录类型", example = "username")
    PrincipalType loginType,

    @NotBlank
    @Schema(description = "身份信息，根据Type变化", example = "flyingjack")
    String principal,

    @NotBlank
    @Schema(description = "密码", example = "123456789")
    String password,

    @Nullable
    @Schema(description = "要连接的服务id", example = "a-client")
    String clientId
){ }
