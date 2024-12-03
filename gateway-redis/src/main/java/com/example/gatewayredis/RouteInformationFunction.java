package com.example.gatewayredis;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Component
public class RouteInformationFunction implements Function<Route, Map<String, Object>> {
    @Override
    public Map<String, Object> apply(Route route) {
        Map<String, Object> routeInformation = new HashMap<>();
        routeInformation.put("id", route.getId());
        routeInformation.put("uri", route.getUri());
        routeInformation.put("predicate", route.getPredicate());
        // 필터를 직렬화하는데 실패하여 클래스명만 출력
        List<String> filtersValues = route.getFilters().stream().map(Object::toString).toList();
        routeInformation.put("filters", filtersValues);
        return routeInformation;
    }
}
