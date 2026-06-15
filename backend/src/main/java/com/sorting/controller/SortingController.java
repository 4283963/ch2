package com.sorting.controller;

import com.sorting.dto.ScanRequest;
import com.sorting.dto.SortingResult;
import com.sorting.service.SortingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/sorting")
@CrossOrigin(origins = "*")
public class SortingController {

    private static final Logger log = LoggerFactory.getLogger(SortingController.class);

    @Autowired
    private SortingService sortingService;

    @PostMapping("/scan")
    public ResponseEntity<SortingResult> scanPackage(@RequestBody ScanRequest request) {
        log.info("收到扫描请求: 条码={}", request.getBarcode());
        SortingResult result = sortingService.processScan(request);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/stats/today")
    public ResponseEntity<Map<String, Object>> getTodayStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("scannedCount", sortingService.getTodayScannedCount());
        stats.put("sortedCount", sortingService.getTodaySortedCount());
        return ResponseEntity.ok(stats);
    }
}
