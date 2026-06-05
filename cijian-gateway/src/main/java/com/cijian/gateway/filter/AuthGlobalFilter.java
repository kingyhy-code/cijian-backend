package com.cijian.gateway.filter;

import com.cijian.common.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements WebFilter, Ordered {

    private final JwtUtil jwtUtil;

    private static final Set<String> PUBLIC_POST_PATHS = Set.of(
            "/api/user/register",
            "/api/user/login",
            "/api/user/forgot-password",
            "/api/user/reset-password",
            "/api/user/token/refresh",
            "/api/user/verify-email"
    );

    private static final Set<String> PUBLIC_GET_PREFIXES = Set.of(
            "/api/content/",
            "/api/search/",
            "/api/profile/",
            "/api/interaction/comment/work/",
            "/api/interaction/annotation/work/",
            "/api/interaction/follow/following/",
            "/api/interaction/follow/followers/",
            "/api/interaction/follow/count/",
            "/api/interaction/like/sentence-praise"
    );

    private static final Set<String> PUBLIC_ANY_PATHS = Set.of(
            "/api/agent/health",
            "/api/user/verify-email"
    );

    private static final Set<String> PUBLIC_PATH_PREFIXES = Set.of(
            "/swagger-ui",
            "/v3/api-docs",
            "/webjars",
            "/v3/api-docs/swagger-config",
            "/uploads"
    );

    /** Agent 公开路径（无需认证） */
    private static final Set<String> AGENT_PUBLIC_PATHS = Set.of(
            "/api/agent/health"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        if (isPublicPath(path, method)) {
            String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                Long userId = jwtUtil.getUserIdFromToken(authHeader.substring(7));
                if (userId != null) {
                    return chain.filter(withUserId(exchange, userId));
                }
            }
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "缺少认证信息");
        }

        String token = authHeader.substring(7);
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            return unauthorized(exchange, "Token无效或已过期");
        }

        return chain.filter(withUserId(exchange, userId));
    }

    private boolean isPublicPath(String path, HttpMethod method) {
        if (PUBLIC_ANY_PATHS.contains(path) || AGENT_PUBLIC_PATHS.contains(path)) {
            return true;
        }
        for (String prefix : PUBLIC_PATH_PREFIXES) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }
        if (method == HttpMethod.POST && PUBLIC_POST_PATHS.contains(path)) {
            return true;
        }
        if (method == HttpMethod.GET && path.matches("^/api/user/\\d+$")) {
            return true;
        }
        if (method == HttpMethod.GET) {
            for (String prefix : PUBLIC_GET_PREFIXES) {
                if (path.startsWith(prefix)) {
                    return true;
                }
            }
        }
        return false;
    }

    private ServerWebExchange withUserId(ServerWebExchange exchange, Long userId) {
        var req = exchange.getRequest().mutate()
                .header("X-User-Id", String.valueOf(userId))
                .build();
        return exchange.mutate().request(req).build();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = String.format("{\"code\":401,\"message\":\"%s\"}", message);
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
