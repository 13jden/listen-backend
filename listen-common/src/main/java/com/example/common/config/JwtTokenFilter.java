package com.example.common.config;

import com.example.common.constants.Constants;
import com.example.common.redis.RedisComponent;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * JWT Token 认证过滤器
 * 从 Cookie 中获取 token 并验证
 */
public class JwtTokenFilter extends OncePerRequestFilter {

    private final RedisComponent redisComponent;

    public JwtTokenFilter(RedisComponent redisComponent) {
        this.redisComponent = redisComponent;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String token = getTokenFromCookie(request);

        if (token != null && !token.isEmpty()) {
            // 验证管理员 token
            String adminAccount = redisComponent.getAdminToken(token);
            if (adminAccount != null) {
                UsernamePasswordAuthenticationToken authentication =
                        new UsernamePasswordAuthenticationToken(
                                "admin:" + adminAccount,  // principal
                                null,                      // credentials
                                Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"))
                        );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // 验证小程序用户 token
                var tokenInfo = redisComponent.getTokenInfo(token);
                if (tokenInfo != null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    "user:" + tokenInfo.getUserId(),  // principal
                                    null,                               // credentials
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"))
                            );
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * 从 Cookie 中获取 token
     */
    private String getTokenFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (Constants.TOKEN_ADMIN.equals(cookie.getName()) || Constants.TOKEN_WX.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
