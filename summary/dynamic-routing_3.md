# Dynamic Routing... #3 - Implementation - InMemoryRouteDefinitionRepository

여기서부터 Spring Cloud Gateway 에서 Dynamic Routing 을 구현하는 방법에 대해 정리하겠다. 

구현은 앞에서 설명한 두가지 모두 샘플로 구현할 예정이며, 이번에는 InMemoryRouteDefinitionRepository 를 구현할 것이다.

## 구성

먼저 샘플 프로젝트를 구성하겠다. Spring Module 을 추가하겠다. 모듈명은 `gateway-in-memory` 로 하겠다.

- Spring Boot Initializr 를 이용해서 프로젝트를 생성했다.
- 컴파일 도구는 Gradle 로 선택했다.

### build.gradle

먼저 Spring Cloud Gateway 에 필요한 의존성을 주입하겠다.

```groovy
dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-gateway'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'org.junit.platform:junit-platform-launcher'
}
```

### application.yaml 

혹시 모를 서버 포트 충돌에 대비해서 포트를 변경하겠다.

```yaml
server:
  port: 8081
```

## 기능 구성 요구사항

1. Dynamic Routing 을 제공하기 위해서는 Routing 정보를 과리하기 위한 기능
   - Route 정보의 저장, 조회, 삭제하기 위한 기능

2. Dynamic Routing 등록과 조회를 위한 API 제공

## Dynamic Route Administrator Service 구현

Dynamic Routing 을 관리하기 위한 서비스를 구현하겠다.

### Route 등록 

먼저 Route 를 등록하는 기능을 구현하겠다.

```java
@Service
@Validated
@RequiredArgsConstructor
public class DynamicRouteAdministratorService {
   private final RouteDefinitionRepository inMemoryRouteDefinitionRepository;
   private final ApplicationEventPublisher publisher;

   public Mono<Void> addRoute(@NotNull RouteDefinition route) {
      Mono<RouteDefinition> mono = Mono.just(route);
      return routeDefinitionWriter.save(mono).then(Mono.defer(() -> {
         publisher.publishEvent(new RefreshRoutesEvent(this));
         return Mono.empty();
      }));
   }
}
```

아래는 Route 를 등록하기위한 컨트롤러이다.

```java
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/routes/in-memory")
public class DynamicRouteAdministratorController {
   private final DynamicRouteAdministratorService dynamicRouteService;
   // Route 추가
   @PostMapping
   public Flux<RouteDefinition> addRoute(@RequestBody RouteDefinition routeDefinition) {
      return dynamicRouteService.addRoute(routeDefinition);
   }
}
```

자 이제 등록을 해보자. 여기서는 IntelliJ 에서 제공하는 http client 를 이용해서 테스트 해보겠다.

```http request
POST /v1/routes/in-memory
Content-Type: application/json
Host: localhost:8081

{
  "id": "application-route",
  "uri": "http://localhost:8080",
  "predicates": [
    {
      "name": "Path",
      "args": {
        "pattern": "/application/**"
      }
    }
  ],
  "filters": [
    {
      "name": "RewritePath",
      "args": {
        "regexp": "/application/(?<segment>.*)",
        "replacement": "/v1/application/${segment}"
      }
    }
  ]
}
```

위와 같이 요청을 하면 Route 가 등록된다. 아래는 응답값이다. 

```http request
POST http://localhost:8081/v1/routes/in-memory

HTTP/1.1 200 OK
content-length: 0

<Response body is empty>

Response code: 200 (OK); Time: 75ms (75 ms); Content length: 0 bytes (0 B)
```
하지만 이 코드가 정말 원하는곳에서 정상적으로 등록되는지 확인은 불가능하다. 이를 확인하기위해 조회기능을 구현해보겠다. 

### Route 조회

먼저 조회를 위해 서비스 코드에 아래의 메서드를 추가하자.

```java
public class DynamicRouteAdministratorService {
    ...

    private final RouteDefinitionLocator routeDefinitionLocator;
    private final RouteLocator routeLocator;

    ...
    public Flux<RouteDefinition> getRoutesDefinitions() {
        return routeDefinitionLocator.getRouteDefinitions();
    }

   
    public Flux<Route> getRoutes() {
        return this.routeLocator.getRoutes();
    }
}
```

그리고 아래와 같이 컨트롤러를 추가하자.

```java
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/routes/in-memory")
public class DynamicRouteAdministratorController {
    
    ...

   @GetMapping
   public Flux<RouteDefinition> getRouteDefinitions() {
      return dynamicRouteService.getRoutesDefinitions();
   }

   @GetMapping("/route")
   public Flux<Map<String, Object>> getRoutes() {
      return dynamicRouteService.getRoutes().map(this.routeInformationFunction);
   }
}

```

자 이제 앞서 구현한 API 를 통해 Route 를 등록하고 아래의 API 를 통해 조회해보자.

```http request
### Get route definition
GET /v1/routes/in-memory
Host: localhost:8081
Content-Type: application/json
```

아래와 같이 응답이 오면 정상적으로 Route 가 등록된 것이다.

```http request
GET http://localhost:8081/v1/routes/in-memory

HTTP/1.1 200 OK
transfer-encoding: chunked
Content-Type: application/json

[
  {
    "id": "application-route",
    "predicates": [
      {
        "name": "Path",
        "args": {
          "pattern": "/application/**"
        }
      }
    ],
    "filters": [
      {
        "name": "RewritePath",
        "args": {
          "regexp": "/application/(?<segment>.*)",
          "replacement": "/v1/application/${segment}"
        }
      }
    ],
    "uri": "http://localhost:8080",
    "metadata": {},
    "order": 0
  }
]
```

`GET /v1/routes/in-memory/route ` 를 이용해서 실제 라우트도 생성됐는지 확인해보자 (여기서는 정리 하지 않음)

자 이제 해당 라우터가 정상적으로 동작하는지 확인해보기위해 간단한 샘플 컨트롤러를 생성하겠다. 

기존에 생성한 `application-1` 모듈에 아래와 같은 컨트롤러를 추가하겠다.

```java
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/v1/application/{value}")
public class ApplicationController {
    @GetMapping
    public String getApplication(@PathVariable String value) {
        log.debug("GET /v1/application/{}", value);
        return "application: " + value;
    }
}
```

이제 생성된 라우터를 통해 요청을 보내보겠다.

```http request
### 등록된 route에 대해서 요청 테스트
GET /application/hello
Host: localhost:8081
Content-Type: application/json

HTTP/1.1 200 OK
Content-Type: text/plain;charset=UTF-8
Content-Length: 18
Date: Tue, 03 Dec 2024 06:50:11 GMT

application: hello

Response code: 200 (OK); Time: 358ms (358 ms); Content length: 18 bytes (18 B)
```

기대하는 결과가 정상적으로 출력되는거슬 확인할 수 있다. 

## 정리

이번에는 InMemoryRouteDefinitionRepository 를 구현해서 Dynamic Routing 을 구현해보았다. 다음 정리에서 redis 를 이용한 구현을 해보겠다.

