package de.v.gom.sample.gateway.router.predicate;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.cloud.gateway.route.builder.UriSpec;
import org.springframework.stereotype.Component;

@Component
public class FilterPredicate implements Router {

    @Override
    public String id() {
        return "filter-router";
    }

    @Override
    public Buildable<Route> apply(PredicateSpec predicateSpec) {
        return predicateSpec.path("/filter_java/**")
            .filters(this::filters)
            .uri("http://localhost:8080");
    }


    private UriSpec filters(GatewayFilterSpec filterSpec) {
        return filterSpec
            .addRequestHeader("Test-Header", "test")
            .addResponseHeader("Test-Response-Header", "test")
            .rewritePath("/filter_java/(?<segment>.*)", "/filter_test_2/${segment}")
            ;
    }
}
