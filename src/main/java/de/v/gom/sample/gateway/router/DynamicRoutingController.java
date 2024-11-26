package de.v.gom.sample.gateway.router;

import de.v.gom.sample.gateway.router.service.DynamicRouting;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.Map;

@RestController
@RequestMapping(value = "/v1/router")
@RequiredArgsConstructor
public class DynamicRoutingController {
    private final DynamicRouting dynamicRouting;
    private final RouteInformationFunction routeInformationFunction;
    @GetMapping
    public Flux<RouteDefinition> getAll() {
        return dynamicRouting.gatAll();
    }

    @GetMapping("/predicate")
    public Flux<Map<String, Object>> getRoutePredicate() {
        return dynamicRouting.getAllPredicate()
            .map(this.routeInformationFunction);
    }

    @GetMapping("/inmemory")
    public Flux<RouteDefinition> getRouteInMemory() {
        return this.dynamicRouting.getAllInMemory();
    }

}
