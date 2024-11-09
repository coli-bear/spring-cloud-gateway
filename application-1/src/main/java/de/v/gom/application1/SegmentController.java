package de.v.gom.application1;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController("/segment")
public class SegmentController {
    @GetMapping("/{stringValue}")
    public String getSegment(@PathVariable String stringValue) {
        return "segment " + stringValue;
    }
}
