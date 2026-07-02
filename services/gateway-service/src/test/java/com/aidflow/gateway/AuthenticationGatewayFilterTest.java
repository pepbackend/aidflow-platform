package com.aidflow.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class AuthenticationGatewayFilterTest {
    @Test
    void skipsAuthenticationForAuthPaths() {
        AuthenticationGatewayFilter filter = new AuthenticationGatewayFilter(
                WebClient.builder().exchangeFunction(unusedExchangeFunction()),
                "http://identity"
        );
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/auth/login").build()
        );
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(chain.called).isTrue();
    }

    @Test
    void addsUserHeadersWhenIdentityServiceAuthenticatesRequest() {
        UUID userId = UUID.randomUUID();
        AuthenticationGatewayFilter filter = new AuthenticationGatewayFilter(
                WebClient.builder().exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                        .header(HttpHeaders.CONTENT_TYPE, "application/json")
                        .body("""
                                {"id":"%s","email":"user@example.com","roles":["ADMIN","VOLUNTEER"]}
                                """.formatted(userId))
                        .build())),
                "http://identity"
        );
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .build()
        );
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(chain.called).isTrue();
        assertThat(chain.exchange.getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo(userId.toString());
        assertThat(chain.exchange.getRequest().getHeaders().getFirst("X-User-Email")).isEqualTo("user@example.com");
        assertThat(chain.exchange.getRequest().getHeaders().getFirst("X-User-Roles")).isEqualTo("ADMIN,VOLUNTEER");
    }

    @Test
    void returnsUnauthorizedWhenIdentityServiceRejectsRequest() {
        AuthenticationGatewayFilter filter = new AuthenticationGatewayFilter(
                WebClient.builder().exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.UNAUTHORIZED).build())),
                "http://identity"
        );
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/tasks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token")
                        .build()
        );
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(chain.called).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getContentType().toString()).isEqualTo("application/json");
        assertThat(exchange.getResponse().getBodyAsString().block())
                .isEqualTo("{\"error\":\"unauthorized\",\"message\":\"Unauthorized\"}");
    }

    @Test
    void returnsUnauthorizedWhenAuthorizationHeaderIsMissing() {
        AuthenticationGatewayFilter filter = new AuthenticationGatewayFilter(
                WebClient.builder().exchangeFunction(unusedExchangeFunction()),
                "http://identity"
        );
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/tasks").build()
        );
        CapturingChain chain = new CapturingChain();

        StepVerifier.create(filter.filter(exchange, chain)).verifyComplete();

        assertThat(chain.called).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getContentType().toString()).isEqualTo("application/json");
        assertThat(exchange.getResponse().getBodyAsString().block())
                .isEqualTo("{\"error\":\"unauthorized\",\"message\":\"Unauthorized\"}");
    }

    private static ExchangeFunction unusedExchangeFunction() {
        return request -> Mono.error(new AssertionError("Identity service should not have been called"));
    }

    private static class CapturingChain implements GatewayFilterChain {
        private boolean called;
        private ServerWebExchange exchange;

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            this.called = true;
            this.exchange = exchange;
            return Mono.empty();
        }
    }
}
