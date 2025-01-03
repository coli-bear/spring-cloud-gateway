package com.example.gatewayparsing.application;

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
