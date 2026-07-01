package com.aidflow.gateway;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticationGatewayFilter implements GlobalFilter, Ordered {
    private static final String AUTH_PATH_PREFIX = "/auth/";

    private final WebClient webClient;

    public AuthenticationGatewayFilter(
            WebClient.Builder webClientBuilder,
            @Value("${identity-service.base-url:http://localhost:8081}") String identityServiceBaseUrl
    ) {
        this.webClient = webClientBuilder.baseUrl(identityServiceBaseUrl).build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith(AUTH_PATH_PREFIX)) {
            return chain.filter(exchange);
        }

        String authorization = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authorization)) {
            return unauthorized(exchange);
        }

        return webClient.get()
                .uri("/internal/auth/me")
                .header(HttpHeaders.AUTHORIZATION, authorization)
                .headers(headers -> {
                    String traceId = exchange.getRequest().getHeaders()
                            .getFirst(TraceIdGatewayFilter.TRACE_ID_HEADER);
                    if (StringUtils.hasText(traceId)) {
                        headers.set(TraceIdGatewayFilter.TRACE_ID_HEADER, traceId);
                    }
                })
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(CurrentUserResponse.class);
                    }
                    return Mono.error(new UnauthorizedException());
                })
                .flatMap(currentUser -> chain.filter(withUserHeaders(exchange, currentUser)))
                .onErrorResume(ignored -> unauthorized(exchange));
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private ServerWebExchange withUserHeaders(ServerWebExchange exchange, CurrentUserResponse currentUser) {
        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .header("X-User-Id", currentUser.id().toString())
                .header("X-User-Email", currentUser.email())
                .header("X-User-Roles", String.join(",", currentUser.roles()))
                .build();

        return exchange.mutate().request(request).build();
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        return exchange.getResponse().setComplete();
    }

    private record CurrentUserResponse(UUID id, String email, List<String> roles) {
    }

    private static class UnauthorizedException extends RuntimeException {
    }
}
