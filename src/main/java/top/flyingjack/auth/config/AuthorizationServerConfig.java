package top.flyingjack.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.server.authorization.JdbcOAuth2AuthorizationService;
import org.springframework.security.oauth2.server.authorization.OAuth2AuthorizationService;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.oauth2.server.authorization.config.annotation.web.configurers.OAuth2AuthorizationServerConfigurer;
import org.springframework.security.oauth2.server.authorization.settings.AuthorizationServerSettings;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.util.StringUtils;
import top.flyingjack.auth.oauth2.handler.Oauth2JsonAuthorizationCodeResponseHandler;
import top.flyingjack.auth.oauth2.repository.CustomOAuth2ClientEntityRepository;
import top.flyingjack.auth.oauth2.repository.CustomOAuth2ClientRepository;
import top.flyingjack.auth.account.handler.LoginAuthenticationFailureHandler;
import top.flyingjack.common.error.GlobalExceptionHandler;

import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;

@Configuration
public class AuthorizationServerConfig {
    private static final Logger log = LoggerFactory.getLogger(AuthorizationServerConfig.class);

    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    public SecurityFilterChain authorizationServerSecurityFilterChain(HttpSecurity http,
                                                                      GlobalExceptionHandler exceptionHandler)
            throws Exception {
        OAuth2AuthorizationServerConfiguration.applyDefaultSecurity(http);
        http.getConfigurer(OAuth2AuthorizationServerConfigurer.class)
                .authorizationEndpoint(authorizationEndpoint ->
                        authorizationEndpoint
                                .authorizationResponseHandler(new Oauth2JsonAuthorizationCodeResponseHandler())
                                .errorResponseHandler(new LoginAuthenticationFailureHandler(exceptionHandler))
                )
                .oidc(Customizer.withDefaults());
        http
                .oauth2ResourceServer((resourceServer) -> resourceServer
                        .jwt(Customizer.withDefaults()));
        return http.build();
    }

    @Bean
    public RegisteredClientRepository registeredClientRepository(
            PasswordEncoder passwordEncoder,
            CustomOAuth2ClientEntityRepository clientEntityRepository,
            ObjectMapper objectMapper,
            @Value("${auth.oauth2.gateway-client.client-id}") String clientId,
            @Value("${auth.oauth2.gateway-client.client-secret}") String clientSecret,
            @Value("${auth.oauth2.gateway-client.redirect-uri}") String redirectUri
    ) {
        CustomOAuth2ClientRepository repository = new CustomOAuth2ClientRepository(clientEntityRepository,
                objectMapper);

        // Bootstrap a default client only when the DB has no clients at all.
        // After first boot, all client management goes through the admin API.
        if (clientEntityRepository.count() == 0) {
            RegisteredClient bootstrapClient = RegisteredClient.withId("0")
                    .clientId(clientId)
                    .clientSecret(passwordEncoder.encode(clientSecret))
                    .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                    .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                    .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                    .redirectUri(redirectUri)
                    .postLogoutRedirectUri("http://gateway/")
                    .scope("openid")
                    .clientSettings(ClientSettings.builder()
                            .requireProofKey(true)
                            .requireAuthorizationConsent(false)
                            .build()
                    )
                    .tokenSettings(TokenSettings
                            .builder().accessTokenTimeToLive(Duration.ofHours(2))
                            .refreshTokenTimeToLive(Duration.ofDays(7))
                            .build()
                    )
                    .build();
            repository.save(bootstrapClient);
            log.info("No OAuth2 clients found — bootstrapped default client '{}'", clientId);
        }

        return repository;
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource(
            @Value("${auth.rsa.private-key:}") String rsaPrivateKeyBase64
    ) {
        KeyPair keyPair;
        if (StringUtils.hasText(rsaPrivateKeyBase64)) {
            keyPair = loadRsaKey(rsaPrivateKeyBase64);
            log.info("RSA signing key loaded from configuration (auth.rsa.private-key)");
        } else {
            log.warn("No RSA private key configured (auth.rsa.private-key / RSA_PRIVATE_KEY). " +
                    "Using ephemeral key — all tokens will be invalidated on restart and " +
                    "multi-instance deployments will break. Set RSA_PRIVATE_KEY in production.");
            keyPair = generateRsaKey();
        }
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
    }

    /**
     * 从 Base64 编码的 PKCS8 私钥字符串加载 RSA KeyPair
     * 生成命令：openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 | openssl pkcs8 -topk8 -nocrypt -outform DER | base64 -w0
     */
    private static KeyPair loadRsaKey(String base64PrivateKey) {
        try {
            byte[] keyBytes = Base64.getMimeDecoder().decode(base64PrivateKey);
            KeyFactory factory = KeyFactory.getInstance("RSA");
            RSAPrivateCrtKey privateKey = (RSAPrivateCrtKey) factory.generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
            RSAPublicKey publicKey = (RSAPublicKey) factory.generatePublic(
                    new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
            return new KeyPair(publicKey, privateKey);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load RSA private key from auth.rsa.private-key", e);
        }
    }

    private static KeyPair generateRsaKey() {
        KeyPair keyPair;
        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(2048);
            keyPair = keyPairGenerator.generateKeyPair();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
        return keyPair;
    }

    /**
     * 用 PostgreSQL 替代默认的 InMemoryOAuth2AuthorizationService。
     * 授权码、access/refresh token 记录跨实例共享，logout 吊销对所有实例生效。
     */
    @Bean
    public OAuth2AuthorizationService authorizationService(
            JdbcTemplate jdbcTemplate,
            RegisteredClientRepository registeredClientRepository) {
        return new JdbcOAuth2AuthorizationService(jdbcTemplate, registeredClientRepository);
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Value("${auth.issuer:}")
    private String issuer;

    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        AuthorizationServerSettings.Builder builder = AuthorizationServerSettings.builder();
        if (StringUtils.hasText(issuer)) {
            builder.issuer(issuer);
        }
        return builder.build();
    }
}
