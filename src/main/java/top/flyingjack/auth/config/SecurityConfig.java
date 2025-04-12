package top.flyingjack.auth.config;

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

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http,
                                                          CorsConfigurationSource corsConfigurationSource,
                                                          JsonAccessDeniedHandler jsonAccessDeniedHandler,
                                                          JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint
    )
            throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // 不使用表单登录，关闭csrf
                .cors(cors -> cors.configurationSource(corsConfigurationSource)) // 设置跨域，这样前端的host就可以直接访问登录了
                .authorizeHttpRequests((authorize) -> authorize
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**")
                        .permitAll() // 放行doc相关接口
                        .requestMatchers("/account/**", "/oauth2/**", "/.well-know/**")
                        .permitAll() // 放行account和oauth2端口
                        .anyRequest()
                        .authenticated()
                )
                // 统一的错误处理
                .exceptionHandling((exceptions) -> exceptions
                        // 授权异常处理（403） - 用户已认证但权限不足时
                        .accessDeniedHandler(jsonAccessDeniedHandler)
                        // 认证异常处理（401） - 用户未认证时访问限制资源
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint)
                )
                // 关闭默认的表单和basic登录
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("*"); // TODO 正式环境应当改为前端的host
        configuration.addAllowedMethod("*");
        configuration.addAllowedHeader("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


    /**
     * 登录流程关键Filter，确保解析LoginRequest
     */
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
        filter.setFilterProcessesUrl("/account/login"); // 哪个endpoints需要使用这个filter - 即登录接口
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
