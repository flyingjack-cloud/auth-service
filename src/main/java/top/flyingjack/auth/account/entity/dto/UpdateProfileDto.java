package top.flyingjack.auth.account.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "更新个人资料请求")
public record UpdateProfileDto(
        @Schema(description = "新用户名（5-15位小写字母和数字）", example = "newname") String username
) {}
