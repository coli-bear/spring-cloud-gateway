package de.v.gom.application1;

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
