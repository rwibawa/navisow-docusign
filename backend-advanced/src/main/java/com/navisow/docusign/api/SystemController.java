package com.navisow.docusign.api;

import java.time.Instant;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
public class SystemController {

    @GetMapping("/status")
    public Map<String, Object> status() {
        return Map.of(
            "service", "backend-advanced",
            "status", "ok",
            "timestamp", Instant.now().toString());
    }
}
