# 02. Response Body Parsing

Spring Cloud Gateway 를 이용해서 요청에 대한 응답값을 Parsing 하는 법에 대해서 정리하겠다. 여기서는 간단하게 코드만 정리하겠다.

> [gateway-parsing](../gateway-parsing) 을 참고하자

## 1. 주요 클래스 

### 1.1. ModifyResponseBodyGatewayFilterFactory

`ModifyResponseBodyGatewayFilterFactory` 는 응답 본문을 변환하기 위해 사용되는 Factory 클래스이다. 구조는 크게 아래와 같이 구성되어있다. 자세한거는 직접 찾아 봅시다.

```java
public class ModifyResponseBodyGatewayFilterFactory extends AbstractGatewayFilterFactory<Config> {
    private final Map<String, MessageBodyDecoder> messageBodyDecoders;
    private final Map<String, MessageBodyEncoder> messageBodyEncoders;
    private final List<HttpMessageReader<?>> messageReaders;

    //...
    
    public GatewayFilter apply(Config config) {
        ModifyResponseGatewayFilter gatewayFilter = new ModifyResponseGatewayFilter(config);
        gatewayFilter.setFactory(this);
        return gatewayFilter;
    }
    
    // ...
    
    public static class Config {
        private Class inClass;
        private Class outClass;
        private Map<String, Object> inHints;
        private Map<String, Object> outHints;
        private String newContentType;
        private RewriteFunction rewriteFunction;

        //...
        
        public Config setRewriteFunction(RewriteFunction rewriteFunction) {
            this.rewriteFunction = rewriteFunction;
            return this;
        }
        
        //...

        protected class ModifiedServerHttpResponse extends ServerHttpResponseDecorator {
        
        }
    }
    // ...
}
```

해당 객체의 주요한 기능은 Http Response Body 의 변경, Json -> XML 변환, 특정 필드의 추가, 삭제 등 다양한 기능을 제공한다.

이 객체를 이용해서 Response Body 를 Parsing 하는 필터를 구현해보겠다.

## 1. 샘플 Application

먼저 아래와 같이 샘플 Application 을 구성하겠다.

- PostRequest.java

```java
/**
 * ResponseBody Parsing 요청용 Request 클래스
 */
public record UserRequest(String id, Data data) {
    public record Data(String name, int age) {
    }
}
```

- RestApiController.java

```java
import com.example.gatewayparsing.application.request.UserRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api")
public class RestApiController {
    @PostMapping
    public UserRequest post(@RequestBody UserRequest request) {
        log.info("post request: {}", request);
        return request;
    }
}
```

간단하게 코드를 설명하자면 `UserRequest` 라는 객체를 받아서 로깅을 한 후 다시 반환하는 코드이다.

자 이제 본격적으로 응답값을 Parsing 하기 위한 코드를 작성해보겠다. 여기 샘플에서는 새로운 필드를 추가하겠다.

## 2. Response Body Parsing GatewayFilter 적용

이제 Response Body Parsing 을 위한 GatewayFilter 를 구현해보겠다.

- CustomFilterPredicate.java

```java
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
```

위 코드는 `RewriteFunction` 을 이용해서 Response Body 를 Parsing 하는 코드이다. 

```json
{
    "id": 1,
    "data": {
        "name": "name",
        "age": 20
    }
}
```

위와 같이 요청을 보내면 아래와 같이 응답을 받도록 구현했다.

```json
{
    "id": 1,
    "username": "name",
    "age": 20
}
```

## 3. Router 등록

이제 구현한 필터를 Router 에 등록해보겠다. 앞에서 정리한 `Router` 를 이용해서 등록했다. 

- RouterConfiguration.java

```java
import com.example.gatewayparsing.gateway.predicate.Router;
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
```

## 4. 적용 결과 

Postman 을 이용해서 아래의 요청정보와 같이 요청을 보내보자.

```http request
POST /custom_filter_java HTTP/1.1
Host: localhost:8083
Content-Type: application/json
Content-Length: 83

{
    "id": "1", 
    "data": {
        "name": "hohog", 
        "age": 22
    }
}
```

우리가 구현한 `CustomFilterPredicate` 를 통해 응답값이 변경된것을 확인할 수 있다. 

```json
{
  "username": "hohog",
  "id": 1,
  "age": 22
}
```

응답 결과가 정상적으로 변경되었으며, 400 에러에대해서도 예외처리가 되는것을 확인할 수 있다. (이거는 테스트 케이스를 추가하지 않겠다.)

이를 활용해서 응답값의 key - value 맵핑된 값의 key 또는 value 를 변경할 수 있는 로직을 작성 할 수 있다. 이제 여기까지 해봤으니 커스터마이징된 필터를 레디스에 등록하는 방법에 대해서 정리해보겠다. 

- [이전: Dynamic Routing #4 - Implementation - RedisRouteDefinitionRepository](./dynamic-routing_4.md)
- [다음: Applying custom filter to Redis](./apply-custom-filter-to-redis.md) ]
