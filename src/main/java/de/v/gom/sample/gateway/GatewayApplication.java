package de.v.gom.sample.gateway;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.event.RefreshRoutesEvent;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionWriter;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}

@Component
@RequiredArgsConstructor
class SampleRoute implements ApplicationRunner {
    private final RouteDefinitionWriter routeDefinitionWriter;
    private final ApplicationEventPublisher publisher;
    @Override
    public void run(ApplicationArguments args) throws Exception {
        this.routeDefinitionWriter.save(Mono.just(new RouteDefinition())
                .then(Mono.defer(() -> {
                    this.publisher.publishEvent(new RefreshRoutesEvent(this));
                    return Mono.empty();
                }))
        );
    }
}