package de.v.gom.sample.gateway.router.predicate;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;

import java.util.function.Function;

public interface Router extends RouterId, Function<PredicateSpec, Buildable<Route>> {
}
