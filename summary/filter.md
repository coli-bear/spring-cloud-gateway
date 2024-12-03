# 02. Filter

Spring Cloud Gateway 에서 제공하는 Filter 의 종류에 대해 정리하겠다. 

전체 38개의 Filter 가 존재하며, 이 중에서 주로 사용되는 Filter 들에 대해 정리하겠다.

## 1. GatewayFilter

HTTP 요청을 처리할 때 라우팅 경로에 대해 적용할 수 있는 필터로 오청을 전처리하거나 후처리하는데 사용된다. 

`Route` 와 연결가능하며 `Route`별로 다양한 필터의 추가가 가능하다. 

### 1.1. 주요기능

1. 요청 들어온 헤더, 쿼리 파라미터, 요청 경로등을 수정할 수 있음.
2. 응담 헤더를 추가하거나 변경 가능
3. 인증요청을 처리하거나 권한을 확인할 수 있음
4. 트래픽 제한을두어 서버의 과부하를 방지할 수 있음
5. 요청 및 응답에 대한 로깅을 수행해 서비스의 상태를 모니터링 할 수 있음.

### 1.2. 주요 Filter

- `AddRequestHeader` : 요청 헤더를 추가하는 필터
- `AddRequestParameter` : 요청 파라미터를 추가하는 필터
- `AddResponseHeader` : 응답 헤더를 추가하는 필터
- `RewritePath` : 요청 경로를 변경하는 필터
- `SetPath` : 요청 경로를 설정하는 필터
- `StripPrefix` : 요청 경로의 prefix 를 제거하는 필터
- `RedirectTo` : 요청을 다른 경로로 리다이렉트 하는 필터
- `Retry` : 요청실패에 대해 재시도하는 필터
- `Hystrix` : Hystrix 를 이용한 Circuit Breaker 를 설정하는 필터(이거는 따로 정리해야겠다..)
- `RequestRateLimiter` : 요청에 대한 제한을 두는 필터

### 1.3 yaml 을 이용한 설정

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
            - Hystrix=circuitbreaker
            - RewritePath=/filter_test/(?<filter_test>.*), /$\{filter_test}
server:
  port: 80
```

테스트를위한 샘플 컨트롤러이다. 

```java
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FilterTestController {
    @GetMapping("/filter_test/{values}")
    public String filterTest(@PathVariable String values) {
        return "Filter test: " + values;
    }
}

```

위 같은 설청으로 `/filter_test/**` 요청을 보내보자



```shell
$ curl -X GET -v http://localhost/filter_test/test      
Note: Unnecessary use of -X or --request, GET is already inferred.
* Host localhost:80 was resolved.
* IPv6: ::1
* IPv4: 127.0.0.1
*   Trying [::1]:80...
* Connected to localhost (::1) port 80
> GET /filter_test/test HTTP/1.1
> Host: localhost
> User-Agent: curl/8.5.0
> Accept: */*
> 
< HTTP/1.1 200 OK
< Content-Type: text/plain;charset=UTF-8
< Content-Length: 12
< Date: Mon, 11 Nov 2024 11:43:16 GMT
< X-Response-Test: testResponse
< 
* Connection #0 to host localhost left intact
segment test%
```
요청을 보냈을때 X-Response-Test 가 추가되어있는것을 확인할 수 있다.
하지만 HttpRequest 에서는 X-Request-Test 가 추가되어있지 않은것을 볼 수 있다 이 부분은 브라우저에서 개발자도구를 이용해 확인해 보자. 

## 1.4. Java 설정을 이용한 Filter 설정

다음은 Java 설정을 이용한 Filter 를 적용해 보겠다. 이때 GatewayFilter 와 GatewayFilterSpec 을 이용해 설정할 수 있다. 

먼저 이전에 만들어둔 `Router` 인터페이스를 상속받은 Predicate 를 생성해주겠다. 

```java
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.cloud.gateway.route.builder.UriSpec;
import org.springframework.stereotype.Component;

@Component
public class FilterPredicate implements Router {

    @Override
    public String id() {
        return "filter-router";
    }

    @Override
    public Buildable<Route> apply(PredicateSpec predicateSpec) {
        return predicateSpec.path("/filter_java/**")
            .filters(this::filters)
            .uri("http://localhost:8080");
    }


    private UriSpec filters(GatewayFilterSpec filterSpec) {
        return filterSpec
            .addRequestHeader("Test-Header", "test")
            .addResponseHeader("Test-Response-Header", "test")
            .rewritePath("/filter_java/(?<segment>.*)", "/filter_test_2/${segment}")
            ;
    }
}

```

이렇게 생성하면 앞에 yaml 설정을 통해 설정한것과 동일한 결과를 얻을 수 있다. 

```shell
$ curl -X GET -v http://localhost/filter_java/test
Note: Unnecessary use of -X or --request, GET is already inferred.
* Host localhost:80 was resolved.
* IPv6: ::1
* IPv4: 127.0.0.1
*   Trying [::1]:80...
* Connected to localhost (::1) port 80
> GET /filter_java/test HTTP/1.1
> Host: localhost
> User-Agent: curl/8.5.0
> Accept: */*
> 
< HTTP/1.1 200 OK
< Content-Type: text/plain;charset=UTF-8
< Content-Length: 19
< Date: Mon, 11 Nov 2024 12:03:19 GMT
< Test-Response-Header: test
< 
* Connection #0 to host localhost left intact
Filter test 2: test
```

이 또한 Request Header 에서는 Test-Header 가 추가되어있지 않지만 Response Header 에서는 Test-Response-Header 가 추가되어있는것을 확인할 수 있다.

Request Header 를 확인하고 싶다면 브라우저에서 개발자도구를 이용해 확인해보자.

이러한 방식으로도 구현할 수 있는데 이것은 CustomFilter 를 만들때 유용하니 참고하자 

먼저 CustomFilter 를 만들어보겠다. 

```java
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

public class CustomBadGatewayFilter implements GatewayFilter {
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        exchange.getResponse().setStatusCode(HttpStatus.BAD_GATEWAY);
        return exchange.getResponse().setComplete(); // 이 처리를 통해서 응답을 완료한다.
    }
}
```

이렇게 만들어진 CustomFilter 를 적용해보겠다. 

```java
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
```

이렇게 요청한 결과는 502 Bad Gateway 가 나오는것을 기대할 수 있다. 아래 결과를 확인해 보자  

```shell
$ curl -X GET -v http://localhost/custom_filter_java/hello
Note: Unnecessary use of -X or --request, GET is already inferred.
* Host localhost:80 was resolved.
* IPv6: ::1
* IPv4: 127.0.0.1
*   Trying [::1]:80...
* Connected to localhost (::1) port 80
> GET /custom_filter_java/hello HTTP/1.1
> Host: localhost
> User-Agent: curl/8.5.0
> Accept: */*
> 
< HTTP/1.1 502 Bad Gateway
< content-length: 0
< 
* Connection #0 to host localhost left intact
```

## 2. Filter 의 순서 

이렇게 등록할 수 있는 필터는 순서를 지정해서 등록할 수 있다 아래 코드를 확인해보자. 

```java
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.GatewayFilterSpec;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class OrderFilterPredicate implements Router {

    @Override
    public String id() {
        return "order-filter-router";
    }

    @Override
    public Buildable<Route> apply(PredicateSpec predicateSpec) {
        return predicateSpec.path("/order_filter_java/**")
            .filters(OrderFilterPredicate::filters)
            .uri("http://localhost:8080");
    }

    private static GatewayFilterSpec filters(GatewayFilterSpec f) {
        return f.filter((exchange, chain) -> {
                log.info(">> Order : 0");
                return chain.filter(exchange);
            }, 0)
            .filter((exchange, chain) -> {
                log.info(">> Order : 1");
                return chain.filter(exchange);
            }, 1)
            .filter((exchange, chain) -> {
                log.info(">> Order : 2");
                ServerHttpResponse response = exchange.getResponse();
                response.setStatusCode(HttpStatus.OK);
                return response.setComplete();
            }, 2);
    }
}
```

먼저 순서를 지정하기 위해서는 `GatewayFilterSpec` 을 이용해 `filter` 를 등록할때 두번째 인자로 순서를 지정할 수 있다.

위 코드를 통해 순서를 지정한 결과를 확인해보자. 

```text
2024-11-11T21:23:21.076+09:00  INFO 95455 --- [ctor-http-nio-2] d.v.g.s.g.p.OrderFilterPredicate         : >> Order : 0
2024-11-11T21:23:21.076+09:00  INFO 95455 --- [ctor-http-nio-2] d.v.g.s.g.p.OrderFilterPredicate         : >> Order : 1
2024-11-11T21:23:21.076+09:00  INFO 95455 --- [ctor-http-nio-2] d.v.g.s.g.p.OrderFilterPredicate         : >> Order : 2
```

콘솔화면에서 순서대로 출력되는것을 확인할 수 있다.

- [이전: Routing](./routing.md)
- [다음: Dynamic Routing #1 - RouteDefinitionLocator](./dynamic-routing_1.md)
