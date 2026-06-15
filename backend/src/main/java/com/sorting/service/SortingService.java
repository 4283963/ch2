package com.sorting.service;

import com.sorting.dto.ScanRequest;
import com.sorting.dto.SortingResult;
import com.sorting.entity.PackageInfo;
import com.sorting.entity.SorterDevice;
import com.sorting.entity.WarehouseRoute;
import com.sorting.repository.PackageInfoRepository;
import com.sorting.repository.SorterDeviceRepository;
import com.sorting.tcp.TcpClient;
import com.sorting.websocket.SortingWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class SortingService {

    private static final Logger log = LoggerFactory.getLogger(SortingService.class);

    @Autowired
    private RouteService routeService;

    @Autowired
    private PackageInfoRepository packageInfoRepository;

    @Autowired
    private SorterDeviceRepository sorterDeviceRepository;

    @Autowired
    private TcpClient tcpClient;

    @Autowired
    private SortingWebSocketHandler webSocketHandler;

    private final ConcurrentHashMap<String, Long> sorterActionCache = new ConcurrentHashMap<>();

    public SortingResult processScan(ScanRequest request) {
        String barcode = request.getBarcode();
        log.info("处理扫描请求: 条码={}, 扫描枪={}", barcode, request.getScannerId());

        SortingResult result = new SortingResult();
        result.setBarcode(barcode);
        result.setScanTime(LocalDateTime.now());

        try {
            WarehouseRoute route = routeService.findRouteByBarcode(barcode);
            if (route == null) {
                result.setSuccess(false);
                result.setMessage("未找到路由信息");
                log.warn("条码 {} 未找到路由", barcode);
                savePackageInfo(barcode, null, "ROUTE_NOT_FOUND", request.getConveyorLine());
                return result;
            }

            result.setDestinationWarehouse(route.getDestinationWarehouse());
            result.setWarehouseCode(route.getWarehouseCode());
            result.setSorterId(route.getSorterId());
            result.setConveyorLine(route.getConveyorLine() != null ? route.getConveyorLine() : request.getConveyorLine());

            Integer sorterId = route.getSorterId();
            boolean commandSuccess = triggerSorter(sorterId, route);

            if (commandSuccess) {
                result.setSuccess(true);
                result.setMessage("分拣成功");
                result.setSortingTime(LocalDateTime.now());
                savePackageInfo(barcode, route, "SORTED", result.getConveyorLine());
                updateSorterStats(sorterId);
            } else {
                result.setSuccess(false);
                result.setMessage("分拣机指令发送失败");
                savePackageInfo(barcode, route, "SORT_FAILED", result.getConveyorLine());
            }

            webSocketHandler.broadcastSortingEvent(result);

        } catch (Exception e) {
            log.error("处理扫描异常: {}", e.getMessage(), e);
            result.setSuccess(false);
            result.setMessage("系统异常: " + e.getMessage());
        }

        return result;
    }

    private boolean triggerSorter(Integer sorterId, WarehouseRoute route) {
        if (sorterId == null) {
            return false;
        }

        Long lastAction = sorterActionCache.get("sorter_" + sorterId);
        long now = System.currentTimeMillis();
        if (lastAction != null && now - lastAction < 200) {
            log.debug("分拣机 {} 动作间隔过短，跳过", sorterId);
            return true;
        }
        sorterActionCache.put("sorter_" + sorterId, now);

        Optional<SorterDevice> deviceOpt = sorterDeviceRepository.findBySorterId(sorterId);
        if (deviceOpt.isPresent()) {
            SorterDevice device = deviceOpt.get();
            String host = device.getTcpHost();
            Integer port = device.getTcpPort();
            if (host != null && port != null) {
                return tcpClient.sendSorterCommand(host, port, sorterId);
            } else {
                return tcpClient.sendSorterCommand(sorterId);
            }
        } else {
            return tcpClient.sendSorterCommand(sorterId);
        }
    }

    private void savePackageInfo(String barcode, WarehouseRoute route, String status, Integer conveyorLine) {
        PackageInfo pkg = new PackageInfo();
        pkg.setBarcode(barcode);
        pkg.setStatus(status);
        pkg.setScanTime(LocalDateTime.now());
        if (route != null) {
            pkg.setDestinationWarehouse(route.getDestinationWarehouse());
            pkg.setWarehouseCode(route.getWarehouseCode());
            pkg.setSorterId(route.getSorterId());
            pkg.setConveyorLine(route.getConveyorLine() != null ? route.getConveyorLine() : conveyorLine);
        } else {
            pkg.setConveyorLine(conveyorLine);
        }
        if ("SORTED".equals(status)) {
            pkg.setSortingTime(LocalDateTime.now());
        }
        packageInfoRepository.save(pkg);
    }

    private void updateSorterStats(Integer sorterId) {
        Optional<SorterDevice> deviceOpt = sorterDeviceRepository.findBySorterId(sorterId);
        if (deviceOpt.isPresent()) {
            SorterDevice device = deviceOpt.get();
            device.setTodaySortCount(device.getTodaySortCount() + 1);
            device.setLastActionTime(LocalDateTime.now());
            sorterDeviceRepository.save(device);
        }
    }

    public long getTodayScannedCount() {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        return packageInfoRepository.countTodayScanned(today);
    }

    public long getTodaySortedCount() {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        return packageInfoRepository.countTodaySorted(today);
    }
}
