package top.flyingjack.auth.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import top.flyingjack.auth.oauth2.handler.Oauth2JsonAuthorizationCodeResponseHandler;
import top.flyingjack.auth.oauth2.repository.CustomOAuth2ClientEntityRepository;
import top.flyingjack.auth.oauth2.repository.CustomOAuth2ClientRepository;
import top.flyingjack.auth.account.handler.LoginAuthenticationFailureHandler;
import top.flyingjack.common.error.GlobalExceptionHandler;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
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
                                // 修改oauth2授权成功默认的跳转行为
                                .authorizationResponseHandler(new Oauth2JsonAuthorizationCodeResponseHandler())
                                // 错误转发给全局处理
                                .errorResponseHandler(new LoginAuthenticationFailureHandler(exceptionHandler))
                )
                .oidc(Customizer.withDefaults());    // Enable OpenID Connect 1.0
        http
                // Accept access tokens for User Info and/or Client Registration
                .oauth2ResourceServer((resourceServer) -> resourceServer
                        .jwt(Customizer.withDefaults()));
        return http.build();
    }

    /**
     * oauth clients信息保存库
     */
    @Bean
    public RegisteredClientRepository registeredClientRepository(PasswordEncoder passwordEncoder,
                                                                 CustomOAuth2ClientEntityRepository clientEntityRepository,
                                                                 ObjectMapper objectMapper) {
        // 先查找是否有存在的client，以clientId为准,如果没有id默认为0
        String clientId = "sample-client";
        RegisteredClient gatewayClient = RegisteredClient.withId("0") // 0表示由数据库生成
                .clientId(clientId)
                .clientSecret(passwordEncoder.encode("sample-secret"))
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .redirectUri("http://localhost:4300/callback")
                .postLogoutRedirectUri("http://gateway/")
                .scope("openid")
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true) // 启用PKCE模式
                        .requireAuthorizationConsent(false) // 关闭授权页面
                        .build()
                )
                .tokenSettings(TokenSettings
                        .builder().accessTokenTimeToLive(Duration.ofHours(2))
                        .refreshTokenTimeToLive(Duration.ofDays(7))
                        .build()
                )
                .build();

        // 返回前保存上述的client
        CustomOAuth2ClientRepository repository = new CustomOAuth2ClientRepository(clientEntityRepository,
                objectMapper);
        repository.save(gatewayClient);
        return repository;
    }

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        KeyPair keyPair = generateRsaKey();
        RSAPublicKey publicKey = (RSAPublicKey) keyPair.getPublic();
        RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
        RSAKey rsaKey = new RSAKey.Builder(publicKey)
                .privateKey(privateKey)
                .keyID(UUID.randomUUID().toString())
                .build();
        JWKSet jwkSet = new JWKSet(rsaKey);
        return new ImmutableJWKSet<>(jwkSet);
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

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }


    @Bean
    public AuthorizationServerSettings authorizationServerSettings() {
        return AuthorizationServerSettings.builder().build();
    }
}
