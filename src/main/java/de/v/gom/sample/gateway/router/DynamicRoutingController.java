package de.v.gom.sample.gateway.router;

import de.v.gom.sample.gateway.router.service.DynamicRouting;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/v1/router")
@RequiredArgsConstructor
public class DynamicRoutingController {
    private final DynamicRouting dynamicRouting;

    @GetMapping
    public Flux<RouteDefinition> getAll() {
        return dynamicRouting.gatAll();
    }

    @GetMapping("/predicate")
    public Flux<Map<String, Object>> getRoutePredicate() {
        return dynamicRouting.getAllPredicate()
            .map(r -> {
                Map<String, Object> rInfo = new HashMap<>();
                rInfo.put("id", r.getId());
                rInfo.put("uri", r.getUri());
                rInfo.put("predicate", r.getPredicate());
                // 필터를 직렬화하는데 실패하여 클래스명만 출력
                rInfo.put("filters", r.getFilters().stream().map(f -> f.getClass().getName()));
                return rInfo;
            });
    }
}
