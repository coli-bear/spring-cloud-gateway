package de.v.gom.sample.gateway.predicate;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.stereotype.Component;

@Component
public class RestControllerPredicate implements Router {
    @Override
    public Buildable<Route> apply(PredicateSpec predicateSpec) {
        return predicateSpec.path("/router/**")
            .filters(f -> f.addRequestHeader("Test-Header", "test"))
            .uri("http://localhost:8080/router");
    }

    @Override
    public String id() {
        return "rest-controller-router";
    }
}
