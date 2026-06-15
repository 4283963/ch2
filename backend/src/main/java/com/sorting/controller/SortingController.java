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
        SortingResult result = sortingService.processScan(request);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/scan/batch")
    public ResponseEntity<Map<String, Object>> scanBatch(@RequestBody ScanRequest[] requests) {
        Map<String, Object> response = new HashMap<>();
        int success = 0;
        int failed = 0;
        long startTime = System.currentTimeMillis();

        for (ScanRequest req : requests) {
            SortingResult r = sortingService.processScan(req);
            if (r.getSuccess()) {
                success++;
            } else {
                failed++;
            }
        }

        long elapsed = System.currentTimeMillis() - startTime;
        response.put("total", requests.length);
        response.put("success", success);
        response.put("failed", failed);
        response.put("elapsedMs", elapsed);
        response.put("qps", requests.length * 1000.0 / Math.max(1, elapsed));

        return ResponseEntity.ok(response);
    }

    @GetMapping("/stats/today")
    public ResponseEntity<Map<String, Object>> getTodayStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("scannedCount", sortingService.getTodayScannedCount());
        stats.put("sortedCount", sortingService.getTodaySortedCount());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/stats/async")
    public ResponseEntity<Map<String, Object>> getAsyncStats() {
        return ResponseEntity.ok(sortingService.getAsyncExecutorStats());
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> getSystemHealth() {
        return ResponseEntity.ok(sortingService.getSystemHealth());
    }
}
