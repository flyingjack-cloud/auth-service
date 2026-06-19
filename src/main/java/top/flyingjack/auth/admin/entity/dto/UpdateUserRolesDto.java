package top.flyingjack.auth.admin.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Set;

@Schema(description = "更新用户角色")
public record UpdateUserRolesDto(
        @Schema(description = "角色ID集合（1=ADMIN, 2=USER, 3=GUEST）") Set<Long> roleIds
) {}
