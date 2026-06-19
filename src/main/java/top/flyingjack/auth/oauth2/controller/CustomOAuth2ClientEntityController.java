package top.flyingjack.auth.oauth2.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import top.flyingjack.auth.oauth2.entity.CustomOauth2ClientEntity;
import top.flyingjack.auth.oauth2.entity.RegisteredClientMapper;
import top.flyingjack.auth.oauth2.entity.dto.ClientSafeDto;
import top.flyingjack.auth.oauth2.entity.dto.CreateClientDto;
import top.flyingjack.auth.oauth2.entity.dto.UpdateClientDto;
import top.flyingjack.auth.oauth2.repository.CustomOAuth2ClientEntityRepository;
import top.flyingjack.auth.oauth2.repository.CustomOAuth2ClientRepository;
import top.flyingjack.common.dto.ApiRes;
import top.flyingjack.common.error.ErrorCode;
import top.flyingjack.common.tool.MessageTool;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;

/**
 * OAuth2 客户端管理 Endpoints
 *
 * @author Zumin Li
 */
@RestController
@RequestMapping("/clients")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "OAuth2 Client管理", description = "查询、新增、修改、删除 OAuth2 注册客户端")
public class CustomOAuth2ClientEntityController {

    private final CustomOAuth2ClientEntityRepository clientEntityRepository;
    private final RegisteredClientRepository registeredClientRepository;
    private final PasswordEncoder passwordEncoder;
    private final MessageSource messageSource;
    private final ObjectMapper objectMapper;

    public CustomOAuth2ClientEntityController(
            CustomOAuth2ClientEntityRepository clientEntityRepository,
            RegisteredClientRepository registeredClientRepository,
            PasswordEncoder passwordEncoder,
            MessageSource messageSource,
            ObjectMapper objectMapper) {
        this.clientEntityRepository = clientEntityRepository;
        this.registeredClientRepository = registeredClientRepository;
        this.passwordEncoder = passwordEncoder;
        this.messageSource = messageSource;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/")
    @Operation(summary = "列出所有已注册客户端")
    public ResponseEntity<ApiRes<List<ClientSafeDto>>> clients() {
        List<ClientSafeDto> list = clientEntityRepository.findAll()
                .stream().map(ClientSafeDto::from).toList();
        return ResponseEntity.ok(ApiRes.success(list));
    }

    @GetMapping("/{client_id}")
    @Operation(summary = "根据 client_id 查询客户端")
    public ResponseEntity<ApiRes<ClientSafeDto>> getClientByClientId(
            @Parameter(description = "客户端id", required = true, example = "sample-client")
            @PathVariable("client_id") String clientId) {
        return clientEntityRepository.findByClientId(clientId)
                .map(e -> ResponseEntity.ok(ApiRes.success(ClientSafeDto.from(e))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiRes.error(HttpStatus.NOT_FOUND.value(),
                                MessageTool.getMessageByContext(messageSource, ErrorCode.CLIENT_NOT_FOUND.getId()))));
    }

    @PostMapping("/")
    @Operation(summary = "创建新客户端")
    public ResponseEntity<ApiRes<ClientSafeDto>> createClient(@RequestBody CreateClientDto dto) {
        if (clientEntityRepository.existsByClientId(dto.clientId())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(ApiRes.error(HttpStatus.CONFLICT.value(),
                            MessageTool.getMessageByContext(messageSource, ErrorCode.OBJECT_CONFLICT.getId())));
        }

        RegisteredClient.Builder builder = RegisteredClient.withId("0")
                .clientId(dto.clientId())
                .clientName(dto.clientName())
                .scopes(s -> Arrays.stream(dto.scopes().split(","))
                        .map(String::trim).forEach(s::add))
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(dto.requireProofKey() != null && dto.requireProofKey())
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(
                                dto.accessTokenTtlHours() != null ? dto.accessTokenTtlHours() : 2))
                        .refreshTokenTimeToLive(Duration.ofDays(
                                dto.refreshTokenTtlDays() != null ? dto.refreshTokenTtlDays() : 7))
                        .build());

        if (StringUtils.hasText(dto.clientSecret())) {
            builder.clientSecret(passwordEncoder.encode(dto.clientSecret()));
        }
        if (StringUtils.hasText(dto.clientAuthenticationMethods())) {
            builder.clientAuthenticationMethods(m -> Arrays.stream(dto.clientAuthenticationMethods().split(","))
                    .map(String::trim).map(ClientAuthenticationMethod::new).forEach(m::add));
        }
        if (StringUtils.hasText(dto.authorizationGrantTypes())) {
            builder.authorizationGrantTypes(t -> Arrays.stream(dto.authorizationGrantTypes().split(","))
                    .map(String::trim).map(AuthorizationGrantType::new).forEach(t::add));
        }
        if (StringUtils.hasText(dto.redirectUris())) {
            builder.redirectUris(u -> Arrays.stream(dto.redirectUris().split(","))
                    .map(String::trim).forEach(u::add));
        }

        registeredClientRepository.save(builder.build());

        // 设置拓展字段（description / avatar / contact）
        clientEntityRepository.findByClientId(dto.clientId()).ifPresent(entity -> {
            entity.setDescription(dto.description());
            entity.setAvatarUrl(dto.avatarUrl());
            entity.setContactEmail(dto.contactEmail());
            clientEntityRepository.save(entity);
        });

        return clientEntityRepository.findByClientId(dto.clientId())
                .map(e -> ResponseEntity.status(HttpStatus.CREATED).body(ApiRes.success(ClientSafeDto.from(e))))
                .orElseGet(() -> ResponseEntity.internalServerError().body(ApiRes.error(500, "Client saved but not found")));
    }

    @PutMapping("/{client_id}")
    @Operation(summary = "更新客户端配置")
    public ResponseEntity<ApiRes<ClientSafeDto>> updateClient(
            @Parameter(description = "客户端id", required = true)
            @PathVariable("client_id") String clientId,
            @RequestBody UpdateClientDto dto) {

        CustomOauth2ClientEntity existing = clientEntityRepository.findByClientId(clientId)
                .orElse(null);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiRes.error(HttpStatus.NOT_FOUND.value(),
                            MessageTool.getMessageByContext(messageSource, ErrorCode.CLIENT_NOT_FOUND.getId())));
        }

        // 更新拓展字段
        if (dto.clientName() != null) existing.setClientName(dto.clientName());
        if (dto.description() != null) existing.setDescription(dto.description());
        if (dto.avatarUrl() != null) existing.setAvatarUrl(dto.avatarUrl());
        if (dto.contactEmail() != null) existing.setContactEmail(dto.contactEmail());
        if (dto.redirectUris() != null) existing.setRedirectUris(dto.redirectUris());
        if (dto.scopes() != null) existing.setScopes(dto.scopes());
        if (dto.authorizationGrantTypes() != null) existing.setAuthorizationGrantTypes(dto.authorizationGrantTypes());
        if (dto.clientAuthenticationMethods() != null) existing.setClientAuthenticationMethods(dto.clientAuthenticationMethods());

        // 更新密钥（提供时才替换）
        if (StringUtils.hasText(dto.clientSecret())) {
            existing.setClientSecret(passwordEncoder.encode(dto.clientSecret()));
        }

        // 更新 TokenSettings（TTL 字段）
        if (dto.accessTokenTtlHours() != null || dto.refreshTokenTtlDays() != null
                || dto.requireProofKey() != null) {
            RegisteredClient current = registeredClientRepository.findByClientId(clientId);
            if (current != null) {
                TokenSettings.Builder tsBuilder = TokenSettings.withSettings(
                        current.getTokenSettings().getSettings());
                if (dto.accessTokenTtlHours() != null)
                    tsBuilder.accessTokenTimeToLive(Duration.ofHours(dto.accessTokenTtlHours()));
                if (dto.refreshTokenTtlDays() != null)
                    tsBuilder.refreshTokenTimeToLive(Duration.ofDays(dto.refreshTokenTtlDays()));
                existing.setTokenSettings(tokenSettingsToJson(tsBuilder.build(), existing));

                if (dto.requireProofKey() != null) {
                    ClientSettings cs = ClientSettings.builder()
                            .requireProofKey(dto.requireProofKey())
                            .requireAuthorizationConsent(false)
                            .build();
                    existing.setClientSettings(clientSettingsToJson(cs, existing));
                }
            }
        }

        clientEntityRepository.save(existing);

        return ResponseEntity.ok(ApiRes.success(ClientSafeDto.from(existing)));
    }

    @DeleteMapping("/{client_id}")
    @Operation(summary = "删除客户端")
    public ResponseEntity<ApiRes<Void>> deleteClient(
            @Parameter(description = "客户端id", required = true)
            @PathVariable("client_id") String clientId) {

        if (!clientEntityRepository.existsByClientId(clientId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiRes.error(HttpStatus.NOT_FOUND.value(),
                            MessageTool.getMessageByContext(messageSource, ErrorCode.CLIENT_NOT_FOUND.getId())));
        }

        ((CustomOAuth2ClientRepository) registeredClientRepository).deleteByClientId(clientId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiRes.success());
    }

    private String tokenSettingsToJson(TokenSettings ts, CustomOauth2ClientEntity fallbackEntity) {
        try {
            return objectMapper.writeValueAsString(ts.getSettings());
        } catch (JsonProcessingException e) {
            return fallbackEntity.getTokenSettings();
        }
    }

    private String clientSettingsToJson(ClientSettings cs, CustomOauth2ClientEntity fallbackEntity) {
        try {
            return objectMapper.writeValueAsString(cs.getSettings());
        } catch (JsonProcessingException e) {
            return fallbackEntity.getClientSettings();
        }
    }
}
