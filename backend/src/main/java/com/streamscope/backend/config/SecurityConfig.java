package com.streamscope.backend.config;

import java.io.IOException;
import java.util.Locale;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;

import com.streamscope.backend.auth.AppUser;
import com.streamscope.backend.auth.UserRepository;

@Configuration
public class SecurityConfig {

    @Bean
    public UserDetailsService userDetailsService(
            UserRepository userRepository
    ) {
        return username -> {
            String normalized = username.toLowerCase(Locale.ROOT);

            AppUser user = userRepository.findByUsername(normalized)
                    .orElseThrow(() ->
                            new UsernameNotFoundException("用户不存在")
                    );

            return User.withUsername(user.username())
                    .password(user.passwordHash())
                    .roles("USER")
                    .build();
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http
    ) throws Exception {
        http
                .csrf(Customizer.withDefaults())

                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()

                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/health",
                                "/actuator/health",
                                "/api/games/**",
                                "/api/streams",
                                "/api/auth/csrf"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/auth/register",
                                "/api/auth/login"
                        ).permitAll()

                        .anyRequest().authenticated()
                )

                .formLogin(form -> form
                        .loginProcessingUrl("/api/auth/login")
                        .successHandler((request, response, authentication) ->
                                writeJson(
                                        response,
                                        200,
                                        "{\"message\":\"登录成功\"}"
                                )
                        )
                        .failureHandler((request, response, exception) ->
                                writeJson(
                                        response,
                                        401,
                                        "{\"code\":\"BAD_CREDENTIALS\",\"message\":\"用户名或密码错误\"}"
                                )
                        )
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/api/auth/logout")
                        .deleteCookies("JSESSIONID")
                        .logoutSuccessHandler((request, response, authentication) ->
                                response.setStatus(204)
                        )
                        .permitAll()
                )

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((request, response, exception) ->
                                writeJson(
                                        response,
                                        401,
                                        "{\"code\":\"AUTH_REQUIRED\",\"message\":\"请先登录\"}"
                                )
                        )
                        .accessDeniedHandler((request, response, exception) ->
                                writeJson(
                                        response,
                                        403,
                                        "{\"code\":\"ACCESS_DENIED\",\"message\":\"请求被拒绝，请检查权限或 CSRF Token\"}"
                                )
                        )
                )

                .requestCache(cache -> cache.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    private static void writeJson(
            HttpServletResponse response,
            int status,
            String body
    ) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(body);
    }
}