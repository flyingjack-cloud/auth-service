package top.flyingjack.auth.oauth2.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * CustomOauth2ClientEntity和RegisteredClient互相转化
 *
 * @author Zumin Li
 * @date 2025/4/4 12:36
 */
public class RegisteredClientMapper {
    private static final Logger log = LoggerFactory.getLogger(RegisteredClientMapper.class);

    private ObjectMapper objectMapper;

    public RegisteredClientMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 将CustomOauth2ClientEntity转化为RegisteredClient
     *
     * @param entity 实体对象，一般由数据库传出
     */
    public RegisteredClient toDomain(CustomOauth2ClientEntity entity) {
        return RegisteredClient.withId(String.valueOf(entity.getId() == null ? "0" : entity.getId()))
                .clientId(entity.getClientId())
                .clientIdIssuedAt(entity.getClientIdIssuedAt())
                .clientSecret(entity.getClientSecret())
                .clientSecretExpiresAt(entity.getClientSecretExpiresAt())
                .clientName(entity.getClientName())
                .clientAuthenticationMethods(authenticationMethods ->
                        Arrays.stream(entity.getClientAuthenticationMethods().split(","))
                                .map(ClientAuthenticationMethod::new)
                                .forEach(authenticationMethods::add))
                .authorizationGrantTypes(grantTypes ->
                        Arrays.stream(entity.getAuthorizationGrantTypes().split(","))
                                .map(AuthorizationGrantType::new)
                                .forEach(grantTypes::add))
                .redirectUris(uris -> uris.addAll(Arrays.asList(entity.getRedirectUris().split(","))))
                .scopes(scopes -> scopes.addAll(Arrays.asList(entity.getScopes().split(","))))
                .clientSettings(ClientSettings.withSettings(stringObjectMap(entity.getClientSettings())).build())
                .tokenSettings(TokenSettings.withSettings(stringObjectMap(entity.getTokenSettings())).build())
                .build();
    }

    /**
     * 将RegisteredClient转化为CustomOauth2ClientEntity
     *
     * @param registeredClient security的registeredClient，一般从运行环境读取
     */
    public CustomOauth2ClientEntity toEntity(RegisteredClient registeredClient) {
        CustomOauth2ClientEntity entity = new CustomOauth2ClientEntity();
        entity.setId(Long.valueOf(registeredClient.getId()));
        entity.setClientId(registeredClient.getClientId());
        entity.setClientIdIssuedAt(registeredClient.getClientIdIssuedAt() == null ? Instant.now() :
                registeredClient.getClientIdIssuedAt());
        entity.setClientSecret(registeredClient.getClientSecret());
        entity.setClientSecretExpiresAt(registeredClient.getClientSecretExpiresAt());
        entity.setClientName(registeredClient.getClientName());

        entity.setClientAuthenticationMethods(
                registeredClient.getClientAuthenticationMethods().stream()
                        .map(ClientAuthenticationMethod::getValue)
                        .collect(Collectors.joining(",")));

        entity.setAuthorizationGrantTypes(
                registeredClient.getAuthorizationGrantTypes().stream()
                        .map(AuthorizationGrantType::getValue)
                        .collect(Collectors.joining(",")));

        entity.setRedirectUris(String.join(",", registeredClient.getRedirectUris()));
        entity.setScopes(String.join(",", registeredClient.getScopes()));
        entity.setClientSettings(mapToString(registeredClient.getClientSettings().getSettings()));
        entity.setTokenSettings(mapToString(registeredClient.getTokenSettings().getSettings()));

        return entity;
    }

    /**
     * 将JSON字符串解析为Map设置
     */
    private Map<String, Object> stringObjectMap(String mapStr) {
        if (!StringUtils.hasText(mapStr)) {
            return Collections.emptyMap();
        }
        try {
            Map<String, Object> settings = this.objectMapper.readValue(mapStr, new TypeReference<Map<String, Object>>() {});

            // 处理Duration无法自动转换的问题
            for (Map.Entry<String, Object> entry : settings.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof String) {
                    try {
                        // 尝试解析为Duration
                        value = Duration.parse((String) value);
                    } catch (Exception e) {
                        // 如果解析失败，保留原值
                    }
                }
                settings.put(entry.getKey(), value);
            }

            return settings;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to parse settings JSON", e);
        }
    }

    /**
     * 将Map设置转换为JSON字符串
     */
    private String mapToString(Map<String, Object> settings) {
        try {
            return this.objectMapper.writeValueAsString(settings);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to convert settings to JSON", e);
        }
    }
}
