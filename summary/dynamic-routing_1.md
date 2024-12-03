# Dynamic Routing... #1 - RouteDefinitionLocator

Spring Cloud Gateway 에서는 라우팅을 동적으로 변경할 수 있는 기능을 제공한다. 이 기능은 `RouteDefinitionLocator` 를 이용하여 라우팅을 변경할 수 있다.

아래에서는 Dynamic Routing 을 구성하기 위해서 필요로하는 주요 인터페이스, 구현체 등을 정리해보고 이를 이용하여 동적 라우팅을 구성하는 방법을 알아보겠다. 

## 1. RouteDefinitionLocator

`RouteDefinitionLocator` 는 라우팅 정보를 제공하는 인터페이스이다. 이 인터페이스를 구현하여 라우팅 정보를 제공할 수 있다.

```java

import reactor.core.publisher.Flux;

public interface RouteDefinitionLocator {
    Flux<RouteDefinition> getRouteDefinitions();
}

```

위 인터페이스를 통해 `Route`의 정의와 구성정보(`Predicate`, `Filter`등)를 제공하며, 기본적으로 `Route`의 추가되거나 삭제될 때 Gateway 에 이를 반영할 수 있도록 변경사항에 대해 추적하는 기능이 있다. 

`RouteDefinitionLocator` 는 기본적을 Reactive Stream 인 `Flux` 를 기반으로 라우팅 정보를 정의한 `RouteDefinition` 을 반환하는 `getRouteDefinitions()` 메서드가 있다. 

## 2. RouteDefinitionLocator 의 주요 구현체 

Spring Cloud Gateway 에서는 `RouteDefinitionLocator` 의 구현체로 `PropertiesRouteDefinitionLocator` 와 `DiscoveryClientRouteDefinitionLocator` 를 제공한다.

### 2.1 PropertiesRouteDefinitionLocator

`PropertiesRouteDefinitionLocator` 는 `application.yml` 또는 `application.properties` 에 정의된 라우팅 정보를 제공하는 구현체이다.

```yaml
spring:
  cloud:
    gateway: # gateway 설정
      routes: # 라우팅 목록 설정
        # route
        - id: segment #url segment
          uri: http://localhost:8080 # 요청을 보낼 서버
          predicates: # 라우팅 조건 설정
            - Path=/segment/** # /segment/ 으로 오는 모든 요청에 대해 처리한다.
            - Method=GET # GET 요청만 처리한다.
          filters:
            - RewritePath=/segment/(?<segment>.*), /$\{segment} # Filter를 통해 요청을 재작성한다. (RewritePath 사용)
        - id: filter_test
          uri: http://localhost:8080
          predicates:
            - Path=/filter_test/**
          filters:
            - AddRequestHeader=X-Request-Test, testRequest # Filter를 통해 요청 헤더를 추가한다. (AddRequestHeader 사용)
            - AddResponseHeader=X-Response-Test, testResponse # Filter를 통해 응답 헤더를 추가한다. (AddResponseHeader 사용)
            - RewritePath=/filter_test/(?<filterTest>.*), /$\{filterTest}
```

위와 같이 `application.yml` 에 라우팅 정보를 정의하고 `PropertiesRouteDefinitionLocator` 를 이용하여 라우팅 정보를 제공한다.

### 2.2 InMemoryRouteDefinitionRepository

`InMemoryRouteDefinitionRepository` 는 메모리에 Route 를 저장하고 관리하는 구현체 이다. 먼저 코드를 한번 살펴보자 

```java

public interface RouteDefinitionWriter {
    Mono<Void> save(Mono<RouteDefinition> route);

    Mono<Void> delete(Mono<String> routeId);
}

public interface RouteDefinitionRepository extends RouteDefinitionLocator, RouteDefinitionWriter {
}

public class InMemoryRouteDefinitionRepository implements RouteDefinitionRepository {
    private final Map<String, RouteDefinition> routes = Collections.synchronizedMap(new LinkedHashMap());

    public InMemoryRouteDefinitionRepository() {
    }

    public Mono<Void> save(Mono<RouteDefinition> route) {
        return route.flatMap((r) -> {
            if (ObjectUtils.isEmpty(r.getId())) {
                return Mono.error(new IllegalArgumentException("id may not be empty"));
            } else {
                this.routes.put(r.getId(), r);
                return Mono.empty();
            }
        });
    }

    public Mono<Void> delete(Mono<String> routeId) {
        return routeId.flatMap((id) -> {
            if (this.routes.containsKey(id)) {
                this.routes.remove(id);
                return Mono.empty();
            } else {
                return Mono.defer(() -> {
                    return Mono.error(new NotFoundException("RouteDefinition not found: " + routeId));
                });
            }
        });
    }

    public Flux<RouteDefinition> getRouteDefinitions() {
        Map<String, RouteDefinition> routesSafeCopy = new LinkedHashMap(this.routes);
        return Flux.fromIterable(routesSafeCopy.values());
    }
}
```

이후에 학습할 `RouteDefinitionWriter`와  `RouteDefinitionLocator` 를 활용하여 라우팅 정보를 저장하고 제공한다.

이 구현체의 특징은 API 를 통해 동적으로 추가된 Route를 관리하고 애플리케이션이 종료시에 저장된 Route 는 삭제된다는 점이다. 

자세한 코드는 Dynamic Route 를 등록하는 곳에서 자세히 설명하겠다.

### 2.3 DiscoveryClientRouteDefinitionLocator 

`DiscoveryClientRouteDefinitionLocator` 는 `Eureka` 나 `Consul` 등의 서비스 디스커버리 클라이언트를 이용하여 라우팅 정보를 제공하는 구현체이다.

각 서비스에 대해 Prefix 기반의 Route 를 생성하게 된다. 예를 들어 `serviceId` 가 `service` 인 서비스에 대해 `/service/**` 로 라우팅 정보를 생성한다.
또한, 서비스 레지스트리의 변경사항(등록/해제)에 따라 Gateway 의 Route 도 자동으로 갱신된다.


### 2.4. Custom RouteDefinitionLocator

사용자가 임의로 정의하여 `RouteDefinitionLocator` 를 구현할 수 있다. 이를 통해 사용자가 원하는 방식으로 라우팅 정보를 제공할 수 있다.

이후에 Redis 를 활용해 동적 라우팅을 구성하게 될 예정이며, 이때 자세하게 다루겠다.

## 3. RouteDefinitionLocator 를 이용한 RouteDefinition 조회

`RouteDefinitionLocator` 를 이용하여 라우팅 정보를 조회하는 방법은 다음과 같다.

### 3.1. RouteDefinition 조회를 위한 Service 구성

먼저 `RouteDefinitionLocator` 를 이용하여 라우팅 정보를 조회하는 서비스를 구성한다. 아래는 서비스에서 제공하기 위한 기능을 분류한 인터페이스이다.

```java
// DynamicRoutingReader.java
public interface DynamicRoutingReader {
    Flux<RouteDefinition> gatAll();
}

// DynamicRoutingWriter.java
public interface DynamicRoutingWriter {
    void addRoute(String id, String uri, String path) throws URISyntaxException;
    void deleteRoute(String id);
    void updateRoute(String id, String uri, String path);
}

// DynamicRouting.java
public interface DynamicRouting extends DynamicRoutingReader, DynamicRoutingWriter {
}

```

- `DynamicRoutingReader` : 라우팅 정보를 조회하는 기능을 제공하는 인터페이스
- `DynamicRoutingWriter` : 라우팅 정보를 추가, 삭제, 수정하는 기능을 제공하는 인터페이스
- `DynamicRouting` : `DynamicRoutingReader` 와 `DynamicRoutingWriter` 를 상속받아 라우팅 정보를 조회, 추가, 삭제, 수정하는 기능을 제공하는 인터페이스

위 인터페이스를 구현하여 라우팅 정보를 조회, 추가, 삭제, 수정하는 서비스를 구성한다.

> 참고: 여기서는 `DynamicRoutingReader` 의 기능만 먼저 implements 해서 구현했다. 

```java
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class DynamicRoutingService implements DynamicRouting {

    private final RouteDefinitionLocator routeDefinitionLocator;

    @Override
    public Flux<RouteDefinition> gatAll() {
        return routeDefinitionLocator.getRouteDefinitions();
    }

    @Override
    public void addRoute(String id, String uri, String path) throws URISyntaxException {
        // TODO: 아래에서 RouteDefinitionWriter 정리에서 구현할 예정이다. #1 
    }

    @Override
    public void deleteRoute(String id) {
        // TODO: 아래에서 RouteDefinitionWriter 정리에서 구현할 예정이다. #2
    }

    @Override
    public void updateRoute(String id, String uri, String path) {
        // TODO: 아래에서 RouteDefinitionWriter 정리에서 구현할 예정이다. #3
    }
}
```

위와 같이 `RouteDefinitionLocator` 를 이용하여 라우팅 정보를 조회하는 서비스를 구성하였다. 다음으로는 이를 호출하는 컨트롤러를 구성한다.

```java
@RestController
@RequestMapping(value = "/v1/router")
@RequiredArgsConstructor
public class DynamicRoutingController {
    private final DynamicRouting dynamicRouting;

    @GetMapping
    public Flux<RouteDefinition> getAll() {
        return dynamicRouting.gatAll();
    }
}
```

`RouteDefinitionLocator` 를 이용해서 라우트 정보를 조회할 때 `RouteDefinition` 을 반환하게 되는데 이는 Spring Webflux 기반의 비동기 스트림인 `Flux<RouteDefinition>` 을 반환한다.

아래는 Postman 을 이용해 조회한 결과이다. 

```json
[
    {
        "id": "segment",
        "predicates": [
            {
                "name": "Path",
                "args": {
                    "_genkey_0": "/segment/**"
                }
            },
            {
                "name": "Method",
                "args": {
                    "_genkey_0": "GET"
                }
            }
        ],
        "filters": [
            {
                "name": "RewritePath",
                "args": {
                    "_genkey_0": "/segment/(?<segment>.*)",
                    "_genkey_1": "/$\\{segment}"
                }
            }
        ],
        "uri": "http://localhost:8080",
        "metadata": {},
        "order": 0
    },
    {
        "id": "filter_test",
        "predicates": [
            {
                "name": "Path",
                "args": {
                    "_genkey_0": "/filter_test/**"
                }
            }
        ],
        "filters": [
            {
                "name": "AddRequestHeader",
                "args": {
                    "_genkey_0": "X-Request-Test",
                    "_genkey_1": "testRequest"
                }
            },
            {
                "name": "AddResponseHeader",
                "args": {
                    "_genkey_0": "X-Response-Test",
                    "_genkey_1": "testResponse"
                }
            },
            {
                "name": "RewritePath",
                "args": {
                    "_genkey_0": "/filter_test/(?<filterTest>.*)",
                    "_genkey_1": "/$\\{filterTest}"
                }
            }
        ],
        "uri": "http://localhost:8080",
        "metadata": {},
        "order": 0
    }
]
```

처리 결과를 보면 이전에 `application.yaml`에서 등록한 라우팅 정보가 조회되는 것을 확인할 수 있다.
혹시나 하는 마음에 해당 내용을 제거하고 조회했더니 아무것도 조회되지 않았다.

> 참고: 이전에 내용 정리하면서 등록한 Predicate(Router)가 조회될 것으로 예상했지만 아무것도 조회되지 않았다. 

`RouteDefinitionWriter` 를 이용한 Route 추가, 삭제, 수정을 통해 동적 라우팅을 구성하기전에 이 부분부터 정리하고 넘어가야겠다.

위 코드를 Debug 해봐도 도대체 어느지점에서 라우팅 정보를 조회하는지 알 수 없었다. 그래서 ChatGPT에게 물어보았다. 

> 질문 : RouteLocatorBuilder 를 통해 등록된 Route 정보를 조회하는 방법이 있을까?
> 
> 답변 : RouteLocatorBuilder 를 통해 등록된 Route 정보는 RouteDefinitionLocator 를 통해 조회할 수 없다. 
> 이유 : RouteDefinitionLocator 는 주로 YAML, JSON 파일, Database 또는 동적으로 추가한 Route 를 관리하는 개체이다. 

앞부분의 복습이지만 앞에서 정의한 라우팅 정보를 한번 코드로 보겠다. 

```java
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
```java
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
```

ChatGPT 가 정리해 준 내용을 보면 이렇게 `PredicateSpec`을 통해 등록된 Route 는 `RouteLocator` 를 사용해 조회해야 한다고 말한다. 그래서 아래와 같이 Controller 를 추가하였다.

```java
public interface DynamicRoutingReader {
    Flux<RouteDefinition> gatAll();
    Flux<Route> getAllPredicate(); // 추가 
}
```
아래는 서비스와 컨트롤러이다.

```java

@Service
@RequiredArgsConstructor
public class DynamicRoutingService implements DynamicRouting {

    ...

    @Override
    public Flux<Route> getAllPredicate() {
        return routeLocator.getRoutes();
    }

    ...
}


@RestController
@RequestMapping(value = "/v1/router")
@RequiredArgsConstructor
public class DynamicRoutingController {

    ...
    
    @GetMapping("/predicate")
    public Flux<Route> getRoutePredicate() {
        return dynamicRouting.getAllPredicate();
    }
}
```

`/v1/router/predicate` 로 요청을 보내서 PredicateSpec 을 통해 등록된 Route 정보를 조회해보자 

> 그냥 조회하면 필터에 대해 직렬화 오류가 발생해서 아래와 같이 코드를 약간 수정해 주었다. 

```java
@RestController
@RequestMapping(value = "/v1/router")
@RequiredArgsConstructor
public class DynamicRoutingController {

    ...
    
    // TOBE: 직렬화 오류로 인해 Map<String, Object> 로 변경
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
```

아래는 조회결과이다. 

```json 
[
    {
        "predicate": {
            "config": null
        },
        "id": "custom-filter-router",
        "filters": [
            "org.springframework.cloud.gateway.filter.OrderedGatewayFilter"
        ],
        "uri": "http://localhost:8080"
    },
    {
        "predicate": {
            "config": null
        },
        "id": "filter-router",
        "filters": [
            "org.springframework.cloud.gateway.filter.OrderedGatewayFilter",
            "org.springframework.cloud.gateway.filter.OrderedGatewayFilter",
            "org.springframework.cloud.gateway.filter.OrderedGatewayFilter"
        ],
        "uri": "http://localhost:8080"
    },
    {
        "predicate": {
            "config": null
        },
        "id": "mvc-controller-router",
        "filters": [
            "org.springframework.cloud.gateway.filter.OrderedGatewayFilter"
        ],
        "uri": "http://localhost:8080/mvc/router"
    },
    {
        "predicate": {
            "config": null
        },
        "id": "order-filter-router",
        "filters": [
            "org.springframework.cloud.gateway.filter.OrderedGatewayFilter",
            "org.springframework.cloud.gateway.filter.OrderedGatewayFilter",
            "org.springframework.cloud.gateway.filter.OrderedGatewayFilter"
        ],
        "uri": "http://localhost:8080"
    },
    {
        "predicate": {
            "config": null
        },
        "id": "rest-controller-router",
        "filters": [
            "org.springframework.cloud.gateway.filter.OrderedGatewayFilter"
        ],
        "uri": "http://localhost:8080/router"
    }
]
```
필터의 이름이 원하는대로 나오지는 않았지만 원하는 정보들이 모두 출력된것을 확인할 수 있다. 

이제 `RouteDefinitionWriter` 를 이용하여 Route 를 추가, 삭제, 수정하는 방법을 알아보겠다.

> 너무 길어서 다음 글에서 정리하겠다.