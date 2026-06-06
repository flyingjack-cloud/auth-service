package top.flyingjack.auth.admin.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "更新用户账号状态")
public record UpdateUserStatusDto(
        @Schema(description = "是否启用（null 表示不修改）") Boolean enabled,
        @Schema(description = "是否解锁（null 表示不修改）") Boolean accountNonLocked
) {}
