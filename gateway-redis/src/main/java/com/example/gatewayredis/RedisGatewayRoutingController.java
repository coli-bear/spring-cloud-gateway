package com.example.gatewayredis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/v1/routes/redis")
@RequiredArgsConstructor
public class RedisGatewayRoutingController {
    private final RedisGatewayRoutingService redisGatewayRoutingService;
    private final RouteInformationFunction routeInformationFunction;

    @PostMapping
    public Mono<Void> routeDefinitionFlux(@RequestBody RouteDefinition route) {
        return redisGatewayRoutingService.addRoute(route);
    }

    @GetMapping
    public Flux<RouteDefinition> getRouteDefinitions() {
        return redisGatewayRoutingService.getRoutesDefinitions();
    }

    @GetMapping("/route")
    public Flux<Map<String, Object>> getRoutes() {
        return redisGatewayRoutingService.getRoutes().map(this.routeInformationFunction);
    }
}
