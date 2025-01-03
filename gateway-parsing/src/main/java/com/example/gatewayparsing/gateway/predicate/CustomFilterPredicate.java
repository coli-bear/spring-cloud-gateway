package com.example.gatewayparsing.gateway.predicate;

import com.example.gatewayparsing.gateway.filter.HttpResponseBodyParsingGatewayFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomFilterPredicate implements Router {
    private final HttpResponseBodyParsingGatewayFilter httpResponseBodyParsingGatewayFilter;

    @Override
    public String id() {
        return "custom-filter-router";
    }

    @Override
    public Buildable<Route> apply(PredicateSpec predicateSpec) {
        return predicateSpec
            .method(HttpMethod.POST).and()
            .path("/custom_filter_java")
            .filters(f -> f.filter(httpResponseBodyParsingGatewayFilter.apply(c -> {
                }))
                .rewritePath("/custom_filter_java", "/api")
            )
            // CustomBadGatewayFilter 필터 등록
            .uri("http://localhost:8083/api");
    }
}
