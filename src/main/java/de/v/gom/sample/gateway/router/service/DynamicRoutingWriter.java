package de.v.gom.sample.gateway.router.service;

import java.net.URISyntaxException;

public interface DynamicRoutingWriter {
    void addRoute(String id, String uri, String path) throws URISyntaxException;
    void deleteRoute(String id);
    void updateRoute(String id, String uri, String path);
}
