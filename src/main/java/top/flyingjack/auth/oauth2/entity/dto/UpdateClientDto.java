package top.flyingjack.auth.oauth2.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "更新OAuth2客户端请求")
public record UpdateClientDto(
        @Schema(description = "客户端名称") String clientName,

        @Schema(description = "新密钥明文（不传则保留原密钥）") String clientSecret,

        @Schema(description = "授权类型，逗号分隔") String authorizationGrantTypes,

        @Schema(description = "认证方法，逗号分隔") String clientAuthenticationMethods,

        @Schema(description = "回调URI，逗号分隔") String redirectUris,

        @Schema(description = "Scope，逗号分隔") String scopes,

        @Schema(description = "是否要求PKCE") Boolean requireProofKey,

        @Schema(description = "AccessToken有效期（小时）") Integer accessTokenTtlHours,

        @Schema(description = "RefreshToken有效期（天）") Integer refreshTokenTtlDays,

        @Schema(description = "描述") String description,
        @Schema(description = "头像URL") String avatarUrl,
        @Schema(description = "联系邮箱") String contactEmail
) {}
