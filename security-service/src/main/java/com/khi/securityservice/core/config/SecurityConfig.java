package com.khi.securityservice.core.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.khi.securityservice.core.exception.RestAccessDeniedHandler;
import com.khi.securityservice.core.exception.RestAuthenticationEntryPoint;
import com.khi.securityservice.core.filter.JwtLogoutFilter;
import com.khi.securityservice.core.filter.JwtReissueFilter;
import com.khi.securityservice.core.handler.LoginSuccessHandler;
import com.khi.securityservice.core.repository.UserRepository;
import com.khi.securityservice.core.service.LoginService;
import com.khi.securityservice.core.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.ExceptionTranslationFilter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.logout.LogoutFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final AuthenticationConfiguration authenticationConfiguration;

    private final LoginService loginService;
    private final LoginSuccessHandler loginSuccessHandler;

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final JwtUtil jwtUtil;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable);

        http
                .formLogin(AbstractHttpConfigurer::disable);

        http
                .httpBasic(AbstractHttpConfigurer::disable);

        http
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(new RestAuthenticationEntryPoint())
                        .accessDeniedHandler(new RestAccessDeniedHandler())
                );

        http
                .oauth2Login((oauth2) -> oauth2
                        .userInfoEndpoint((userInfoEndpointConfig) -> userInfoEndpointConfig
                                .userService(loginService))
                        .successHandler(loginSuccessHandler));

        http
                .addFilterAfter(jwtReissueFilter(), ExceptionTranslationFilter.class);

        http
                .addFilterAt(jwtLogoutFilter(), LogoutFilter.class);

        http
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers(HttpMethod.POST, "/security/join").permitAll()
                        .requestMatchers(HttpMethod.POST, "/security/jwt/reissue").permitAll()
                        .requestMatchers("/actuator/**").permitAll()

                        /* Swagger */
                        .requestMatchers(HttpMethod.GET, "/swagger-ui/index.html").permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui/swagger-ui.css").permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui/index.css").permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui/swagger-ui-bundle.js").permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui/swagger-ui-standalone-preset.js").permitAll()
                        .requestMatchers(HttpMethod.GET, "/swagger-ui/swagger-initializer.js").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v3/api-docs/swagger-config").permitAll()
                        .requestMatchers(HttpMethod.GET, "/v3/api-docs").permitAll()

                        .requestMatchers(HttpMethod.GET, "/security/test").permitAll()

                        /* API */
                        .requestMatchers(HttpMethod.GET, "/security/user/profile").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/security/user/additional-info").permitAll()

                        /* Internal Feign API for chat-service */
                        .requestMatchers("/security/users/**").permitAll()
                        .requestMatchers("/security/feign/user/nickname").permitAll()
                        .requestMatchers("/security/user/*").permitAll()

                        .requestMatchers("/security/admin").hasRole("ADMIN")

                        .anyRequest().authenticated());

        http
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        return http.build();
    }

    @Bean
    public JwtReissueFilter jwtReissueFilter() {

        return new JwtReissueFilter(jwtUtil, redisTemplate, objectMapper);
    }

    @Bean
    public JwtLogoutFilter jwtLogoutFilter() {

        return new JwtLogoutFilter(jwtUtil, redisTemplate, objectMapper);
    }
}