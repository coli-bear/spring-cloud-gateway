package de.v.gom.sample.gateway.predicate;

import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.builder.Buildable;
import org.springframework.cloud.gateway.route.builder.PredicateSpec;

import java.util.function.Function;

public interface Router extends Function<PredicateSpec, Buildable<Route>> {
    String id();
}
