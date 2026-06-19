package top.flyingjack.auth.oauth2.entity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import top.flyingjack.auth.oauth2.entity.CustomOauth2ClientEntity;

import java.time.Instant;

@Schema(description = "OAuth2客户端信息（不含密钥原文）")
public record ClientSafeDto(
        Long id,
        String clientId,
        Instant clientIdIssuedAt,
        String clientName,
        String clientAuthenticationMethods,
        String authorizationGrantTypes,
        String redirectUris,
        String scopes,
        String description,
        String avatarUrl,
        String contactEmail
) {
    public static ClientSafeDto from(CustomOauth2ClientEntity entity) {
        return new ClientSafeDto(
                entity.getId(),
                entity.getClientId(),
                entity.getClientIdIssuedAt(),
                entity.getClientName(),
                entity.getClientAuthenticationMethods(),
                entity.getAuthorizationGrantTypes(),
                entity.getRedirectUris(),
                entity.getScopes(),
                entity.getDescription(),
                entity.getAvatarUrl(),
                entity.getContactEmail()
        );
    }
}
