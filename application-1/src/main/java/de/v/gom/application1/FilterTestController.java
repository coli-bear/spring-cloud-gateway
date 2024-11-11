package de.v.gom.application1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FilterTestController {
    @GetMapping("/filter_test/{values}")
    public String filterTest(@PathVariable String values) {
        return "Filter test: " + values;
    }
    @GetMapping("/filter_test_2/{values}")
    public String filterTest2(@PathVariable String values) {
        return "Filter test 2: " + values;
    }
}
