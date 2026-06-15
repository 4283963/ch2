package com.sorting.controller;

import com.sorting.service.AntiWearService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/antiwear")
@CrossOrigin(origins = "*")
public class AntiWearController {

    private static final Logger log = LoggerFactory.getLogger(AntiWearController.class);

    @Autowired
    private AntiWearService antiWearService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        return ResponseEntity.ok(antiWearService.getStatus());
    }

    @PostMapping("/enable")
    public ResponseEntity<Map<String, Object>> enableAntiWear() {
        antiWearService.setAntiWearEnabled(true);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "减磨模式已开启");
        result.put("data", antiWearService.getStatus());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/disable")
    public ResponseEntity<Map<String, Object>> disableAntiWear() {
        antiWearService.setAntiWearEnabled(false);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("message", "减磨模式已关闭");
        result.put("data", antiWearService.getStatus());
        return ResponseEntity.ok(result);
    }

    @PostMapping("/toggle")
    public ResponseEntity<Map<String, Object>> toggleAntiWear() {
        boolean current = antiWearService.isAntiWearEnabled();
        antiWearService.setAntiWearEnabled(!current);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("enabled", !current);
        result.put("message", !current ? "减磨模式已开启" : "减磨模式已关闭");
        result.put("data", antiWearService.getStatus());
        return ResponseEntity.ok(result);
    }
}
