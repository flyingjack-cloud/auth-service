package top.flyingjack.auth.account.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "修改密码请求")
public record ChangePasswordDto(
        @NotBlank
        @Schema(description = "当前密码") String oldPassword,

        @NotBlank
        @Schema(description = "新密码（8-16位非空字符）") String newPassword
) {}
