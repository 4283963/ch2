package com.sorting.service;

import com.sorting.dto.ScanRequest;
import com.sorting.dto.SortingResult;
import com.sorting.entity.WarehouseRoute;
import com.sorting.repository.PackageInfoRepository;
import com.sorting.tcp.AsyncSortingExecutor;
import com.sorting.websocket.SortingWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class SortingService {

    private static final Logger log = LoggerFactory.getLogger(SortingService.class);

    @Autowired
    private RouteCacheService routeCacheService;

    @Autowired
    private AsyncSortingExecutor asyncSortingExecutor;

    @Autowired
    private PackageInfoRepository packageInfoRepository;

    @Autowired
    private SortingWebSocketHandler webSocketHandler;

    public SortingResult processScan(ScanRequest request) {
        long startTime = System.nanoTime();
        String barcode = request.getBarcode();

        SortingResult result = new SortingResult();
        result.setBarcode(barcode);
        result.setScanTime(LocalDateTime.now());

        try {
            WarehouseRoute route = routeCacheService.findRouteByBarcode(barcode);

            result = asyncSortingExecutor.submitTask(barcode, route, request.getConveyorLine());

            long elapsed = (System.nanoTime() - startTime) / 1000;
            if (elapsed > 10000) {
                log.warn("扫描处理耗时过长: {}μs, 条码={}", elapsed, barcode);
            }

            log.debug("扫描处理完成: 条码={}, 耗时={}μs, 分拣机={}", barcode, elapsed, result.getSorterId());

        } catch (Exception e) {
            log.error("处理扫描异常: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("系统异常: " + e.getMessage());
        }

        return result;
    }

    public long getTodayScannedCount() {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        return packageInfoRepository.countTodayScanned(today);
    }

    public long getTodaySortedCount() {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        return packageInfoRepository.countTodaySorted(today);
    }

    public Map<String, Object> getAsyncExecutorStats() {
        return asyncSortingExecutor.getStats();
    }

    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new java.util.HashMap<>();
        health.put("cacheRoutes", routeCacheService.getRouteCount());
        health.put("cacheSorters", routeCacheService.getSorterCount());
        health.put("todayScanned", getTodayScannedCount());
        health.put("todaySorted", getTodaySortedCount());
        health.put("asyncExecutor", asyncSortingExecutor.getStats());
        health.put("websocketConnections", webSocketHandler.getConnectionCount());
        return health;
    }
}
