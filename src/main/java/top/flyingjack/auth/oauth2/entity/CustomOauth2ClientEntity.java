package top.flyingjack.auth.oauth2.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;

/**
 * 存储在数据库的Oauth2 Client，需要隐射到RegisteredClient才能注册
 *
 * @author Zumin Li
 * @date 2025/4/4 11:33
 */
@Schema(name = "CustomOauth2ClientEntity", description = "存储在数据库的Oauth2 Client, 包含一些额外信息")
@Entity
@Table(name = "oauth2_registered_client")
public class CustomOauth2ClientEntity {
    @Id
    @NotBlank
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true)
    private String clientId;

    @Column
    @CreatedDate
    private Instant clientIdIssuedAt;

    private String clientSecret;

    private Instant clientSecretExpiresAt;

    @NotBlank
    private String clientName;

    @NotBlank
    @Column(length = 1000)
    private String clientAuthenticationMethods;

    @NotBlank
    @Column(length = 1000)
    private String authorizationGrantTypes;

    @Column(length = 1000)
    private String redirectUris;

    @NotBlank
    @Column(length = 1000)
    private String scopes;

    @NotBlank
    @Column(length = 2000)
    private String clientSettings;

    @NotBlank
    @Column(length = 2000)
    private String tokenSettings;

    // 以下字段为拓展字段
    @Column(length = 500)
    private String description;
    @Column(length = 1000)
    private String avatarUrl;
    private String contactEmail;

    public Long getId() {
        return id;
    }

    public String getClientId() {
        return clientId;
    }

    public Instant getClientIdIssuedAt() {
        return clientIdIssuedAt;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public Instant getClientSecretExpiresAt() {
        return clientSecretExpiresAt;
    }

    public String getClientName() {
        return clientName;
    }

    public String getClientAuthenticationMethods() {
        return clientAuthenticationMethods;
    }

    public String getAuthorizationGrantTypes() {
        return authorizationGrantTypes;
    }

    public String getRedirectUris() {
        return redirectUris;
    }

    public String getScopes() {
        return scopes;
    }

    public String getClientSettings() {
        return clientSettings;
    }

    public String getTokenSettings() {
        return tokenSettings;
    }

    public String getDescription() {
        return description;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setClientIdIssuedAt(Instant clientIdIssuedAt) {
        this.clientIdIssuedAt = clientIdIssuedAt;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setClientSecretExpiresAt(Instant clientSecretExpiresAt) {
        this.clientSecretExpiresAt = clientSecretExpiresAt;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public void setClientAuthenticationMethods(String clientAuthenticationMethods) {
        this.clientAuthenticationMethods = clientAuthenticationMethods;
    }

    public void setAuthorizationGrantTypes(String authorizationGrantTypes) {
        this.authorizationGrantTypes = authorizationGrantTypes;
    }

    public void setRedirectUris(String redirectUris) {
        this.redirectUris = redirectUris;
    }

    public void setScopes(String scopes) {
        this.scopes = scopes;
    }

    public void setClientSettings(String clientSettings) {
        this.clientSettings = clientSettings;
    }

    public void setTokenSettings(String tokenSettings) {
        this.tokenSettings = tokenSettings;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }
}
