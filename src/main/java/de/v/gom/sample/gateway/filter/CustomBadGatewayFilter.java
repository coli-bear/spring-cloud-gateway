package de.v.gom.sample.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class CustomBadGatewayFilter implements GatewayFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getResponse().setStatusCode(HttpStatus.BAD_GATEWAY);
        return exchange.getResponse().setComplete(); // 이 처리를 통해서 응답을 완료한다.
    }
}
