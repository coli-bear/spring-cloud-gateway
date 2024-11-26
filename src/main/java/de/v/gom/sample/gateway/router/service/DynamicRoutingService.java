package de.v.gom.sample.gateway.router.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.InMemoryRouteDefinitionRepository;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.net.URISyntaxException;

@Service
@RequiredArgsConstructor
public class DynamicRoutingService implements DynamicRouting {

    private final RouteDefinitionLocator routeDefinitionLocator;
    private final RouteLocator routeLocator;
    private final InMemoryRouteDefinitionRepository inMemoryRouteDefinitionRepository;

    @Override
    public Flux<RouteDefinition> gatAll() {
        return routeDefinitionLocator.getRouteDefinitions();
    }

    @Override
    public Flux<Route> getAllPredicate() {
        return routeLocator.getRoutes();
    }

    @Override
    public Flux<RouteDefinition> getAllInMemory() {
        return inMemoryRouteDefinitionRepository.getRouteDefinitions();
    }

    @Override
    public void addRoute(String id, String uri, String path) throws URISyntaxException {

    }

    @Override
    public void deleteRoute(String id) {

    }

    @Override
    public void updateRoute(String id, String uri, String path) {

    }
}
