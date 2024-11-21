package de.v.gom.sample.gateway.router.predicate;

import de.v.gom.sample.gateway.filter.CustomBadGatewayFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.stereotype.Component;

@Component
public class CustomFilterPredicate implements Router {
        @Override
        public String id() {
            return "custom-filter-router";
        }

        @Override
        public Buildable<Route> apply(PredicateSpec predicateSpec) {
            return predicateSpec.path("/custom_filter_java/**")
                .filters(f -> f.filter(new CustomBadGatewayFilter())) // CustomBadGatewayFilter 필터 등록
                .uri("http://localhost:8080");
        }
}
