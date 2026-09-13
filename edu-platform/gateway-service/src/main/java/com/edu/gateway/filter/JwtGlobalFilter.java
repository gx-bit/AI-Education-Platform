package com.edu.gateway.filter;

import com.edu.common.security.model.LoginUser;
import com.edu.common.security.util.JwtUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * JWT 全局认证过滤器
 * 在网关层统一验证JWT，并将用户信息转发给下游服务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Value("#{'${gateway.white-list}'.split(',')}")
    private List<String> whiteList;

    private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        String optionalToken = getToken(exchange.getRequest());
        if (isWhiteList(exchange.getRequest()) && !StringUtils.hasText(optionalToken)) {
            return chain.filter(exchange);
        }

        // Authenticated calls to public endpoints still carry identity downstream.
        String token = optionalToken;
        if (!StringUtils.hasText(token)) {
            return unauthorized(exchange, "未提供认证Token，请先登录");
        }

        // 验证Token
        if (!jwtUtil.validateToken(token)) {
            return unauthorized(exchange, "Token无效或已过期，请重新登录");
        }

        // 解析用户信息，注入Header转发给下游
        LoginUser loginUser = jwtUtil.parseToken(token);
        if (loginUser == null) {
            return unauthorized(exchange, "Token解析失败");
        }

        ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                .header("X-User-Id", String.valueOf(loginUser.getUserId()))
                .header("X-User-Name", loginUser.getUsername() != null ? loginUser.getUsername() : "")
                .header("X-User-Role", loginUser.getRole() != null ? loginUser.getRole() : "")
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -100; // 最高优先级
    }

    private boolean isWhiteList(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        if (HttpMethod.GET.equals(request.getMethod())) {
            List<String> publicCourseQueries = List.of(
                    "/api/course/list", "/api/course/category/list", "/api/course/recommend",
                    "/api/course/{id}", "/api/course/{id}/goto", "/api/course/{id}/outline",
                    "/api/course/{id}/interaction", "/api/course/{id}/reviews");
            if (publicCourseQueries.stream().anyMatch(pattern -> PATH_MATCHER.match(pattern, path))) {
                return true;
            }
        }
        return whiteList.stream().anyMatch(pattern -> path.equals(pattern) || PATH_MATCHER.match(pattern, path));
    }

    private String getToken(ServerHttpRequest request) {
        String bearerToken = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = Map.of(
                "code", 401,
                "message", message,
                "timestamp", System.currentTimeMillis()
        );

        try {
            String json = objectMapper.writeValueAsString(body);
            DataBuffer buffer = response.bufferFactory()
                    .wrap(json.getBytes(StandardCharsets.UTF_8));
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException e) {
            return response.setComplete();
        }
    }
}
