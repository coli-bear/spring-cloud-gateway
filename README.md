# Spring Cloud Gateway 

> 토이프로젝트(PiGaHub)인 API Gateway 구현을 위한 Spring Cloud Gateway 학습

Spring Cloud Gateway 에 대한 설명은 [Spring Cloud Gateway](https://spring.io/projects/spring-cloud-gateway)를 참고하시기 바랍니다.

## Application 구조 

```text
root -|
      |- src : Spring Cloud Gateway 의 소스코드가 존재 
      |- application-1 : Spring Cloud Gateway 가 라우팅 하기 위한 서버 1
      |- gateway-in-memory : Spring Cloud Gateway 의 InMemoryRouteDefinitionRepository 기반의 Dynamic Routing 을 위한 샘플 프로젝트
      |- gateway-redis : Spring Cloud Gateway 의 RedisRouteDefinitionRepository 기반의 Dynamic Routing 을 위한 샘플 프로젝트
```
## 필요 의존성 주입

```groovy
dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-gateway'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'
}
```

## 기능 정리 목차 

- [라우팅에 대한 정리](./summary/routing.md)
- [필터에 대한 정리](./summary/filter.md)
- [Dynamic Routing #1 - RouteDefinitionLocator](./summary/dynamic-routing_1.md)
- [Dynamic Routing #2 - RouteDefinitionWriter](./summary/dynamic-routing_2.md)
- [Dynamic Routing #3 - Implementation - InMemoryRouteDefinitionRepository](./summary/dynamic-routing_3.md)
- [Dynamic Routing #4 - Implementation - RedisRouteDefinitionRepository](./summary/dynamic-routing_4.md)
