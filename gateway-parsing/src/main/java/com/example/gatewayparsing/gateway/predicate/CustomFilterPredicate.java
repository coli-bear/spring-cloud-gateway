package com.example.gatewayparsing.gateway.predicate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.factory.rewrite.RewriteFunction;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

@Slf4j
@Component
@RequiredArgsConstructor
public class CustomFilterPredicate implements Router {
    private final ObjectMapper objectMapper;

    @Override
    public String id() {
        return "custom-filter-router";
    }

    @Override
    public Buildable<Route> apply(PredicateSpec predicateSpec) {
        return predicateSpec
            .method(HttpMethod.POST).and()
            .path("/custom_filter_java")
            .filters(f ->
                f
                    .modifyResponseBody(String.class, String.class, getStringStringRewriteFunction())
                    .rewritePath("/custom_filter_java", "/api")
            )
            // CustomBadGatewayFilter 필터 등록
            .uri("http://localhost:8083/api");
    }

    private RewriteFunction<String, String> getStringStringRewriteFunction() {
        return (exchange, s) -> {

            if (Objects.requireNonNull(exchange.getResponse().getStatusCode()).is4xxClientError()) {
                log.error("Client Exception: {}", s);
                return Mono.error(new ResponseStatusException(exchange.getResponse().getStatusCode(), s));
            }
            try {

                JsonNode jsonNode = this.objectMapper.readTree(s);
                int id = jsonNode.path("id").asInt();
                String username = jsonNode.path("data").path("name").asText();
                int age = jsonNode.path("data").path("age").asInt();

                log.info("id: {}, username: {}, age: {}", id, username, age);
                Map<String, Object> newBody = Map.of("id", id, "username", username, "age", age);
                return Mono.just(this.objectMapper.writeValueAsString(newBody));

            } catch (NullPointerException e) {
                log.error("Server Exception: {}", e.getMessage());
                return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Response Status is not found : " + e.getMessage()));
            } catch (JsonProcessingException e) {
                log.error("JsonParsing Error: {}", e.getMessage());
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "JsonParsing Error : " + e.getMessage()));
            }

        };
    }
}
