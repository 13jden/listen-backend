package com.example.common.config;

import com.example.common.redis.RedisComponent;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final RedisComponent redisComponent;

    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    public SecurityConfig(RedisComponent redisComponent) {
        this.redisComponent = redisComponent;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF（前后端分离使用 token 认证）
                .csrf(AbstractHttpConfigurer::disable)
                // 不使用 session
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 配置请求授权
                .authorizeHttpRequests(auth -> auth
                        // ==================== 公开接口（无需登录） ====================
                        // 管理员登录
                        .requestMatchers("/admin/login").permitAll()
                        //验证码
                        .requestMatchers("/checkcode").permitAll()
                        // 小程序用户登录注册
                        .requestMatchers("/user/login", "/user/register").permitAll()
                        // 获取验证码
                        .requestMatchers("/checkCode/**").permitAll()
                        // 静态资源
                        .requestMatchers("/static/**", "/assets/**", "/css/**", "/js/**", "/images/**").permitAll()
                        // 允许所有 OPTIONS 请求（跨域预检）
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        // ==================== 其他请求需要认证 ====================
                        .anyRequest().authenticated()
                )
                // 添加 JWT Token 过滤器
                .addFilterBefore(new JwtTokenFilter(redisComponent), UsernamePasswordAuthenticationFilter.class)
                // 禁用默认登录页（API 不需要）
                .formLogin(AbstractHttpConfigurer::disable)
                // 禁用 HTTP Basic 认证
                .httpBasic(AbstractHttpConfigurer::disable);

        return http.build();
    }
}
