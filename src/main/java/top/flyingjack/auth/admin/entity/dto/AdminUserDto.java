package top.flyingjack.auth.admin.entity.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.Set;

@Schema(description = "用户信息（管理员视图）")
public record AdminUserDto(
        @JsonFormat(shape = JsonFormat.Shape.STRING)
        @Schema(description = "用户ID") Long id,

        @Schema(description = "用户名") String username,
        @Schema(description = "手机号") String phone,
        @Schema(description = "邮箱") String email,
        @Schema(description = "注册时间") Instant createdAt,

        @Schema(description = "是否启用") boolean enabled,
        @Schema(description = "是否未锁定") boolean accountNonLocked,
        @Schema(description = "账号是否未过期") boolean accountNonExpired,
        @Schema(description = "凭证是否未过期") boolean credentialsNonExpired,

        @Schema(description = "角色列表，如 ROLE_USER") Set<String> roles
) {}
