package top.flyingjack.auth.oauth2.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "创建OAuth2客户端请求")
public record CreateClientDto(
        @NotBlank
        @Schema(description = "客户端ID", example = "my-app") String clientId,

        @NotBlank
        @Schema(description = "客户端名称", example = "My Application") String clientName,

        @Schema(description = "客户端密钥明文（存储时BCrypt加密）", example = "secret123") String clientSecret,

        @Schema(description = "授权类型，逗号分隔", example = "authorization_code,refresh_token")
        String authorizationGrantTypes,

        @Schema(description = "认证方法，逗号分隔", example = "client_secret_basic")
        String clientAuthenticationMethods,

        @Schema(description = "回调URI，逗号分隔", example = "http://localhost:3000/callback")
        String redirectUris,

        @NotBlank
        @Schema(description = "Scope，逗号分隔", example = "openid") String scopes,

        @Schema(description = "是否要求PKCE", example = "true") Boolean requireProofKey,

        @Schema(description = "AccessToken有效期（小时）", example = "2") Integer accessTokenTtlHours,

        @Schema(description = "RefreshToken有效期（天）", example = "7") Integer refreshTokenTtlDays,

        @Schema(description = "描述") String description,
        @Schema(description = "头像URL") String avatarUrl,
        @Schema(description = "联系邮箱") String contactEmail
) {}
