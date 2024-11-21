package de.v.gom.sample.gateway.configuration;

import de.v.gom.sample.gateway.router.predicate.Router;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class RouterConfiguration {
    private final List<Router> routers;

    @Bean
    public RouteLocator routerControllerLocator(RouteLocatorBuilder builder) {
        RouteLocatorBuilder.Builder routerLocatorBuilder = builder.routes();
        routers.forEach(router -> {
            log.info("Registering router: {}", router.id());
            routerLocatorBuilder.route(router.id(), router);
        });
        return routerLocatorBuilder
            .build();
    }
}
