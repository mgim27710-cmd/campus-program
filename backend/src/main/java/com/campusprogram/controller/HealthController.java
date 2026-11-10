package com.campusprogram.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

// [2단계-⑥ Controller] 서버가 살아 있는지 확인하는 주소 (배포 후 가장 먼저 확인)
@RestController
public class HealthController {

    @GetMapping("/api/health")
    public Map<String, String> health() {
        return Map.of("status", "UP");
    }
}
