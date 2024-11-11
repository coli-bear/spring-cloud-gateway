package de.v.gom.sample.gateway.predicate;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderFilterPredicate implements Router {

    @Override
    public String id() {
        return "order-filter-router";
    }

    @Override
    public Buildable<Route> apply(PredicateSpec predicateSpec) {
        return predicateSpec.path("/order_filter_java/**")
            .filters(OrderFilterPredicate::filters)
            .uri("http://localhost:8080");
    }

    private static GatewayFilterSpec filters(GatewayFilterSpec f) {
        return f.filter((exchange, chain) -> {
                log.info(">> Order : 0");
                return chain.filter(exchange);
            }, 0)
            .filter((exchange, chain) -> {
                log.info(">> Order : 1");
                return chain.filter(exchange);
            }, 1)
            .filter((exchange, chain) -> {
                log.info(">> Order : 2");
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.OK);
                return response.setComplete();
            }, 2);
    }
}
