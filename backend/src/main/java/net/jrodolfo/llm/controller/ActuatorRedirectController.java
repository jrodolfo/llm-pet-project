package net.jrodolfo.llm.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ActuatorRedirectController {

    @GetMapping("/actuator")
    public String redirectToHealth() {
        return "redirect:/actuator/health";
    }
}
