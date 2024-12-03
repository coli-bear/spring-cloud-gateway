package com.example.gateway;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/routes/in-memory")
public class DynamicRouteAdministratorController {
    private final DynamicRouteAdministratorService dynamicRouteService;
    private final RouteInformationFunction routeInformationFunction;
    // Route 추가
    @PostMapping
    public Mono<Void> addRoute(@RequestBody RouteDefinition routeDefinition) {
        return dynamicRouteService.addRoute(routeDefinition);
    }

    @GetMapping
    public Flux<RouteDefinition> getRouteDefinitions() {
        return dynamicRouteService.getRoutesDefinitions();
    }

    @GetMapping("/route")
    public Flux<Map<String, Object>> getRoutes() {
        return dynamicRouteService.getRoutes().map(this.routeInformationFunction);
    }
}
