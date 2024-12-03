# Dynamic Routing... #2 - RouteDefinitionWriter

이전에는 `RouteDefinitionLocator`(`RouteLocator`) 을 이용해 라우팅 정보를 가져오는 방법을 정리했다. 이번에는 라우팅 정보를 등록, 수정, 삭제하기 위한 방법들에 대해 정리하겠다.

## RouteDefinitionWriter 

Spring Cloud Gateway 에서 새로운 Route 정보를 관리하기 위헤서는 `RouteDefinitionWriter` 를 사용하며, 구조는 아래와 같다. 

```java
package org.springframework.cloud.gateway.route;

import reactor.core.publisher.Mono;

public interface RouteDefinitionWriter {
    Mono<Void> save(Mono<RouteDefinition> route);

    Mono<Void> delete(Mono<String> routeId);
}
```

- `save` : 새로운 Route 정보를 저장한다.
- `delete` : Route 정보를 삭제한다.

## RouteDefinitionRepository

`RouteDefinitionWriter`와 `RouteDefinitionLocator`를 합친 것이 `RouteDefinitionRepository` 이다. 

```java
public interface RouteDefinitionRepository extends RouteDefinitionLocator, RouteDefinitionWriter {
}
```

해당 인터페이스의 주요 구현체로는 `InMemoryRouteDefinitionRepository` 와 `RedisRouteDefinitionRepository` 가 있다.

### InMemoryRouteDefinitionRepository

`RouteDefinition` 정보를 메모리(JVM - Heap) 에 저장하여 관리하는 구현체이다. 먼저 아래 코드를 보자. 

```java
package org.springframework.cloud.gateway.route;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.util.ObjectUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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

`InMemoryRouteDefinitionRepository`는 Collection 에서 제공하는 `SynchronizedMap`을 이용해 `RouteDefinition` 정보를 저장하고 관리한다.

> `SynchronizedMap`은 멀티스레드 환경에서 안전하게 사용할 수 있도록 동기화된 Map 이다. 이는 자바 1.5 이전에 사용하고 이후에는 `ConcurrentHashMap`을 일반적으로 사용한다고 한다. 
> 자세한 내용은 [SynchronizedMap과 ConcurrentHashMap](https://ooz.co.kr/71) 에서 확인할 수 있다.

이 구현체는 JVM 에서 관리되기 때문에 서버가 종료되거나 재시작하면 Route 정보가 초기화된다는 단점이 있다. 따라서 Gateway 구동시 Route 정보를 초기화하는 방법이 필요하다.

### RedisRouteDefinitionRepository

`RouteDefinition` 정보를 Redis 에 저장하여 관리하는 구현체이다. 아래 코드를 보자.

```java
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.support.NotFoundException;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class RedisRouteDefinitionRepository implements RouteDefinitionRepository {
    private static final Logger log = LoggerFactory.getLogger(RedisRouteDefinitionRepository.class);
    private static final String ROUTEDEFINITION_REDIS_KEY_PREFIX_QUERY = "routedefinition_";
    private ReactiveRedisTemplate<String, RouteDefinition> reactiveRedisTemplate;
    private ReactiveValueOperations<String, RouteDefinition> routeDefinitionReactiveValueOperations;

    public RedisRouteDefinitionRepository(ReactiveRedisTemplate<String, RouteDefinition> reactiveRedisTemplate) {
        this.reactiveRedisTemplate = reactiveRedisTemplate;
        this.routeDefinitionReactiveValueOperations = reactiveRedisTemplate.opsForValue();
    }

    public Flux<RouteDefinition> getRouteDefinitions() {
        return this.reactiveRedisTemplate.scan(ScanOptions.scanOptions().match(this.createKey("*")).build()).flatMap((key) -> {
            return this.reactiveRedisTemplate.opsForValue().get(key);
        }).onErrorContinue((throwable, routeDefinition) -> {
            if (log.isErrorEnabled()) {
                log.error("get routes from redis error cause : {}", throwable.toString(), throwable);
            }

        });
    }

    public Mono<Void> save(Mono<RouteDefinition> route) {
        return route.flatMap((routeDefinition) -> {
            return this.routeDefinitionReactiveValueOperations.set(this.createKey(routeDefinition.getId()), routeDefinition).flatMap((success) -> {
                return success ? Mono.empty() : Mono.defer(() -> {
                    return Mono.error(new RuntimeException(String.format("Could not add route to redis repository: %s", routeDefinition)));
                });
            });
        });
    }

    public Mono<Void> delete(Mono<String> routeId) {
        return routeId.flatMap((id) -> {
            return this.routeDefinitionReactiveValueOperations.delete(this.createKey(id)).flatMap((success) -> {
                return success ? Mono.empty() : Mono.defer(() -> {
                    return Mono.error(new NotFoundException(String.format("Could not remove route from redis repository with id: %s", routeId)));
                });
            });
        });
    }

    private String createKey(String routeId) {
        return "routedefinition_" + routeId;
    }
}
```

먼저 Spring 의 `@Repository` 를 통해 Bean 이 등록되고 있는것을 확인할 수 있다. 이 Repository 가 어떻게 등록되는지 먼저 알아보자 

> 이 부분을 자세히 집고 넘어가는 이유는 이후 진행할 프로젝트인 [PiGaHub Gateway](https://github.com/a101201031/pigahub)에서 Redis 를 이용한 동적인 라우트 관리를 위해서이다. 

### RedisRouteDefinitionRepository Bean 등록

먼저 `RedisRouteDefinitionRepository`가 언제 Bean 으로 등록되는지 확인해보자. 

```java
import java.util.List;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RedisRouteDefinitionRepository;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.support.ConfigurationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.web.reactive.DispatcherHandler;

@Configuration(
    proxyBeanMethods = false
)
@AutoConfigureAfter({RedisReactiveAutoConfiguration.class})
@AutoConfigureBefore({GatewayAutoConfiguration.class})
@ConditionalOnBean({ReactiveRedisTemplate.class})
@ConditionalOnClass({RedisTemplate.class, DispatcherHandler.class})
@ConditionalOnProperty(
    name = {"spring.cloud.gateway.redis.enabled"},
    matchIfMissing = true
)
class GatewayRedisAutoConfiguration {
    GatewayRedisAutoConfiguration() {
    }

    @Bean
    public RedisScript redisRequestRateLimiterScript() {
        DefaultRedisScript redisScript = new DefaultRedisScript();
        redisScript.setScriptSource(new ResourceScriptSource(new ClassPathResource("META-INF/scripts/request_rate_limiter.lua")));
        redisScript.setResultType(List.class);
        return redisScript;
    }

    @Bean
    @ConditionalOnMissingBean
    public RedisRateLimiter redisRateLimiter(ReactiveStringRedisTemplate redisTemplate, @Qualifier("redisRequestRateLimiterScript") RedisScript<List<Long>> redisScript, ConfigurationService configurationService) {
        return new RedisRateLimiter(redisTemplate, redisScript, configurationService);
    }

    @Bean
    @ConditionalOnProperty(
        value = {"spring.cloud.gateway.redis-route-definition-repository.enabled"},
        havingValue = "true"
    )
    @ConditionalOnClass({ReactiveRedisTemplate.class})
    public RedisRouteDefinitionRepository redisRouteDefinitionRepository(ReactiveRedisTemplate<String, RouteDefinition> reactiveRedisTemplate) {
        return new RedisRouteDefinitionRepository(reactiveRedisTemplate);
    }

    @Bean
    public ReactiveRedisTemplate<String, RouteDefinition> reactiveRedisRouteDefinitionTemplate(ReactiveRedisConnectionFactory factory) {
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<RouteDefinition> valueSerializer = new Jackson2JsonRedisSerializer(RouteDefinition.class);
        RedisSerializationContext.RedisSerializationContextBuilder<String, RouteDefinition> builder = RedisSerializationContext.newSerializationContext(keySerializer);
        RedisSerializationContext<String, RouteDefinition> context = builder.value(valueSerializer).build();
        return new ReactiveRedisTemplate(factory, context);
    }
}
```

여기서 유의 깊게 봐야 할 곳은 `GatewayRedisAutoConfiguration#redisRouteDefinitionRepository` 이다 해당 메서드의 어노테이션을 살펴보자

두가지의 `@ConditionalOn{XXXX}` 어노테이션이 사용된 것을 확인할 수 있다. 간단하게 어노테이션에 대한 설명을 하고 넘어가자 

- `@ConditionalOnProperty` : 특정 프로퍼티가 존재하거나 특정 값이 매칭되는 경우에 Bean 을 등록한다.
- `@ConditionalOnClass` : 특정 클래스가 존재하는 경우에 Bean 을 등록한다.

이 어노테이션을 정리하면 `spring.cloud.gateway.redis-route-definition-repository.enabled` 프로퍼티가 `true` 이고 ReactiveRedisTemplate 가 존재하는 경우에 `RedisRouteDefinitionRepository` Bean 이 등록된는것을 알 수 있다. 

결국 해당 Bean 을 활성화 하기 위해서는 `ReactiveRedisTemplate` 을 활성화 해주어야 한다. 

#### ReactiveRedisTemplate Bean 

> `ReactiveRedis` 에대해서는 [Reactive Redis란?](https://lsdiary.tistory.com/115) 을 참고자하

`ReactiveRedisTemplate` 을 활성화하기 위해서는 먼저 의존성을 주입해주어야 한다. 

```groovy
...

dependencies {
    ... 
    
    // 추가해주지 
    implementation 'org.springframework.boot:spring-boot-starter-data-redis-reactive'
    
    ...
}

...
```

추가하게 되면 `ReactiveRedisTemplate` 객체가 주입된것을 확인할 수 있다. (사진 캡처 힘드니 직접 객체 찾아봅시다)

