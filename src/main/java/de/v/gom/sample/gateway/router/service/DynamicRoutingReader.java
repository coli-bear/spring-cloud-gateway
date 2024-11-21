package de.v.gom.sample.gateway.router.service;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import reactor.core.publisher.Flux;

public interface DynamicRoutingReader {
    Flux<RouteDefinition> gatAll();
    Flux<Route> getAllPredicate();
}
