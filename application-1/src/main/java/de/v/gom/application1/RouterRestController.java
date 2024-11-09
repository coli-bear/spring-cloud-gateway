package de.v.gom.application1;

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