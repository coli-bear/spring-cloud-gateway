# 01. Routing   

Spring Cloud Gateway 에서 제공하는 라우팅기능을 사용하는 방법은 크게 2 가지로 나눌 수 있다. 

1. **Application.yml** 파일을 이용한 라우팅 설정
2. **Java Configuration** 파일을 이용한 라우팅 설정

아래에 각각의 방법을 예시 코드로 만들어 정리해봤다.

먼저, Spring Cloud Gateway 라우팅을 사용하기 위해 

## 1. Application.yml 파일을 이용한 라우팅 설정

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
server:
  port: 
```

위와 같이 `application.yml` 파일을 이용하여 라우팅 설정을 하게 되며, `spring.cloud.gateway.routes` 에 라우팅 목록을 설정하게 된다.

하지만 이런 방법은 단점이 있는데 라우팅 설정이 많아질 경우, `application.yml` 파일이 매우 길어지게 되어 가독성이 떨어지게 된다.
또한, 동적인 라우팅 설정이 불가능하다. 
Config Server 를 이용해 외부 설정을 가져올 수 있지만 동적으로 생성, 삭제가 불가능하다. 

## 2. Java Configuration 파일을 이용한 라우팅 설정

### 2.1. Router Interface 생성

코드 기반의 Route 를 구성하기 위해서 라우터 규칙을 정의하는 인터페이스를 먼저 생성하겠다.

실제 라우터를 등록하기 위한 코드는 아래와 같은 코드가 존재하는데 이를 `Router` 인터페이스로 정의하여 사용하겠다. 

```java
public class RouteLocatorBuilder {
    // ... 
    
    public static class Builder {

        public Builder route(String id, Function<PredicateSpec, Buildable<Route>> fn) {
            Buildable<Route> routeBuilder = (Buildable)fn.apply((new RouteSpec(this)).id(id));
            this.add(routeBuilder);
            return this;
        }

        public Builder route(Function<PredicateSpec, Buildable<Route>> fn) {
            Buildable<Route> routeBuilder = (Buildable)fn.apply((new RouteSpec(this)).randomId());
            this.add(routeBuilder);
            return this;
        }

        public RouteLocator build() {
            return () -> {
                return Flux.fromIterable(this.routes).map((routeBuilder) -> {
                    return (Route)routeBuilder.build();
                });
            };
        }

    }
    
    //...
}

```

위 코드에서 실제로 라우터를 등록하기위해 `route` 메서드를 이용하여 라우터를 등록해주게 된다. 이때 ID 값을 통해 라우터를 구분하게 되는데 해당 ID 값을 관리하기위해 `Router`인터페이스에 #id() 메서드를 추가했다.

```java
package de.v.gom.sample.gateway.predicate;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;

import java.util.function.Function;

public interface Router extends Function<PredicateSpec, Buildable<Route>> {
    String id();
}
```

### 2.2. Router 구현체 생성

위에서 정의한 `Router` 인터페이스를 구현한 구현체를 생성하겠다. 이때 MVC Controller 와 RestController 에 대해 모두 잘 처리하는지 처리방식이 다른지 비교하기 위해 두개의 구현체를 생성하겠다.

먼저 Mvc Controller 에 대한 라우터를 생성하겠다.

```java
package de.v.gom.sample.gateway.predicate;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.stereotype.Component;

@Component
public class MvcControllerPredicate implements Router {
    @Override
    public Buildable<Route> apply(PredicateSpec predicateSpec) {
        return predicateSpec.path("/mvc/router")
            .uri("http://localhost:8080/mvc/router");
    }

    @Override
    public String id() {
        return "mvc-controller-router";
    }
}
```
Rest Controller 에 대한 라우터를 생성하겠다.

```java
package de.v.gom.sample.gateway.predicate;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;
import org.springframework.stereotype.Component;

@Component
public class RestControllerPredicate implements Router {
    @Override
    public Buildable<Route> apply(PredicateSpec predicateSpec) {
        return predicateSpec.path("/router/**")
            .uri("http://localhost:8080/router");
    }

    @Override
    public String id() {
        return "rest-controller-router";
    }
}
```

### 2.3. Router Configuration 생성

이제 위에서 생성한 라우터 구현체를 이용하여 라우터 설정을 생성하겠다.

```java
package de.v.gom.sample.gateway.configuration;

import de.v.gom.sample.gateway.predicate.Router;
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
            log.debug("Registering router: {}", router.id());
            routerLocatorBuilder.route(router.id(), router);
        });
        return routerLocatorBuilder
            .build();
    }
}
```
### 2.4. Router Configuration 테스트

위에서 생성한 RouterConfiguration 을 테스트하기 위해 간단한 Controller 를 작성하겠다. 

```java
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
public class RouterRestController {
    Long id = 0L;

    @GetMapping("/router")
    public String getRouter() {
        log.debug("GET /router - {}", ++id);
        return "router: " + id;
    }
}

@Slf4j
@Controller
class RouterMvcController {
    Long id = 0L;
    @GetMapping("/mvc/router")
    public String getRouter(Model model) {
        log.debug("GET /mvc/router - {}", ++id);
        model.addAttribute("id", id);
        return "router";
    }
}
```
아래는 MVC Controller 에서 사용할 `router.html` 파일이다. 

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
</head>
<body>
<h1>Router</h1>
<span>this page call count : </span>
<span th:text="${id}"></span>
</body>
</html>
```

이제 요청을 해보겠다. 

```shell
$ curl http://localhost/router
router: 1

$ curl http://localhost/router
router: 2

curl http://localhost/mvc/router
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>Title</title>
</head>
<body>
<h1>Router</h1>
<span>this page call count : </span>
<span>7</span>
</body>
</html>
```

잘 출력되는것을 확인할 수 있다. 
