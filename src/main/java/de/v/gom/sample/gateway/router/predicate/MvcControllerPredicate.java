package de.v.gom.sample.gateway.router.predicate;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.stereotype.Component;

@Component
public class MvcControllerPredicate implements Router {
    @Override
    public Buildable<Route> apply(PredicateSpec predicateSpec) {
        return predicateSpec.path("/mvc/router")
            .filters(f -> f.addRequestHeader("Test-Header", "test"))
            .uri("http://localhost:8080/mvc/router");
    }

    @Override
    public String id() {
        return "mvc-controller-router";
    }
}
