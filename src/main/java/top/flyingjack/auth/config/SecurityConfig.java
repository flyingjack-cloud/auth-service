package top.flyingjack.auth.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import top.flyingjack.auth.feign.client.CaptchaClient;
import top.flyingjack.auth.account.filter.RestAuthenticationFilter;
import top.flyingjack.auth.account.handler.JsonAccessDeniedHandler;
import top.flyingjack.auth.account.handler.JsonAuthenticationEntryPoint;
import top.flyingjack.auth.account.handler.LoginAuthenticationFailureHandler;
import top.flyingjack.auth.account.handler.LoginAuthenticationSuccessHandler;
import top.flyingjack.auth.account.other.LoginAuthenticationProvider;
import top.flyingjack.auth.account.service.LoginAttemptService;
import top.flyingjack.auth.account.service.LoginUserDetailService;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http,
                                                          CorsConfigurationSource corsConfigurationSource,
                                                          JsonAccessDeniedHandler jsonAccessDeniedHandler,
                                                          JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint,
                                                          UsernamePasswordAuthenticationFilter restAuthenticationFilter
    )
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // 不使用表单登录，关闭csrf
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**")
                        .permitAll()
                        .requestMatchers("/actuator/**")
                        .permitAll()
                        // 公开端点：登录/登出/注册/忘记密码/状态检查
                        .requestMatchers(
                                "/account/login", "/account/logout", "/account/check-login",
                                "/account/check/**", "/account/register", "/account/reset-password")
                        .permitAll()
                        // OAuth2 AS 端点全部公开（框架自身负责客户端认证）
                        .requestMatchers("/oauth2/**", "/.well-known/**")
                        .permitAll()
                        // /account/profile, /account/change-password, /admin/** 等需要认证
                        .anyRequest()
                        .authenticated()
                )
                .exceptionHandling((exceptions) -> exceptions
                        .accessDeniedHandler(jsonAccessDeniedHandler)
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint)
                )
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                // 将 RestAuthenticationFilter 注册到 Spring Security 过滤链的正确位置
                .addFilterAt(restAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 防止 Spring Boot 将 RestAuthenticationFilter 再次自动注册到 Servlet 容器过滤链（避免双重执行）
     */
    @Bean
    public FilterRegistrationBean<UsernamePasswordAuthenticationFilter> restAuthenticationFilterRegistration(
            UsernamePasswordAuthenticationFilter restAuthenticationFilter
    ) {
        FilterRegistrationBean<UsernamePasswordAuthenticationFilter> registration =
                new FilterRegistrationBean<>(restAuthenticationFilter);
        registration.setEnabled(false);
        return registration;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${auth.cors.allowed-origins}") String allowedOriginsStr
    ) {
        CorsConfiguration configuration = new CorsConfiguration();
        Arrays.stream(allowedOriginsStr.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .forEach(configuration::addAllowedOriginPattern);
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        configuration.setAllowCredentials(true); // Session cookie 跨域需要
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public UsernamePasswordAuthenticationFilter restAuthenticationFilter(
            AuthenticationManager authenticationManager,
            LoginAuthenticationSuccessHandler loginAuthenticationSuccessHandler,
            LoginAuthenticationFailureHandler loginAuthenticationFailureHandler,
            LoginAttemptService loginAttemptService,
            CaptchaClient captchaClient
    ) {
        RestAuthenticationFilter filter = new RestAuthenticationFilter();
        filter.setAuthenticationManager(authenticationManager);
        filter.setFilterProcessesUrl("/account/login");
        filter.setAuthenticationFailureHandler(loginAuthenticationFailureHandler);
        filter.setAuthenticationSuccessHandler(loginAuthenticationSuccessHandler);
        filter.setLoginAttemptService(loginAttemptService);
        filter.setCaptchaClient(captchaClient);
        return filter;
    }

    @Bean
    public AuthenticationManager authenticationManager(
            LoginUserDetailService userDetailsService,
            PasswordEncoder passwordEncoder,
            LoginAttemptService loginAttemptService
    ) {
        LoginAuthenticationProvider authenticationProvider = new LoginAuthenticationProvider(userDetailsService,
                passwordEncoder, loginAttemptService);
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
