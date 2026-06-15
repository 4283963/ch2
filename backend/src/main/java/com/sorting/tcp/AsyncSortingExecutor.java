package com.sorting.tcp;

import com.sorting.dto.SortingResult;
import com.sorting.entity.PackageInfo;
import com.sorting.entity.SorterDevice;
import com.sorting.entity.WarehouseRoute;
import com.sorting.repository.PackageInfoRepository;
import com.sorting.repository.SorterDeviceRepository;
import com.sorting.service.RouteCacheService;
import com.sorting.websocket.SortingWebSocketHandler;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class AsyncSortingExecutor {

    private static final Logger log = LoggerFactory.getLogger(AsyncSortingExecutor.class);

    private static final int SORTER_THREAD_POOL_SIZE = 10;
    private static final int DB_THREAD_POOL_SIZE = 4;
    private static final int MAX_QUEUE_SIZE = 50;
    private static final long SORTER_MIN_INTERVAL_MS = 150;

    @Autowired
    private TcpClient tcpClient;

    @Autowired
    private RouteCacheService routeCacheService;

    @Autowired
    private PackageInfoRepository packageInfoRepository;

    @Autowired
    private SorterDeviceRepository sorterDeviceRepository;

    @Autowired
    private SortingWebSocketHandler webSocketHandler;

    private final Map<Integer, LinkedBlockingQueue<SortingTask>> sorterQueues = new ConcurrentHashMap<>();
    private final Map<Integer, AtomicLong> sorterLastActionTime = new ConcurrentHashMap<>();
    private final Map<Integer, AtomicLong> sorterSortCount = new ConcurrentHashMap<>();
    private final LinkedBlockingQueue<PackageInfo> dbSaveQueue = new LinkedBlockingQueue<>(1000);

    private ExecutorService sorterExecutor;
    private ExecutorService dbExecutor;
    private volatile boolean running = true;

    private final AtomicLong totalProcessed = new AtomicLong(0);
    private final AtomicLong totalSuccess = new AtomicLong(0);
    private final AtomicLong totalFailed = new AtomicLong(0);
    private final AtomicLong maxQueueWaitMs = new AtomicLong(0);
    private final AtomicLong totalQueueWaitMs = new AtomicLong(0);

    public static class SortingTask {
        String barcode;
        WarehouseRoute route;
        Integer conveyorLine;
        LocalDateTime scanTime;
        long createTime;
        boolean urgent;

        public SortingTask(String barcode, WarehouseRoute route, Integer conveyorLine, boolean urgent) {
            this.barcode = barcode;
            this.route = route;
            this.conveyorLine = conveyorLine;
            this.scanTime = LocalDateTime.now();
            this.createTime = System.currentTimeMillis();
            this.urgent = urgent;
        }
    }

    @PostConstruct
    public void init() {
        sorterExecutor = Executors.newFixedThreadPool(SORTER_THREAD_POOL_SIZE, r -> {
            Thread t = new Thread(r);
            t.setName("sorter-worker-%d".formatted(sorterQueues.size()));
            t.setDaemon(true);
            return t;
        });

        dbExecutor = Executors.newFixedThreadPool(DB_THREAD_POOL_SIZE, r -> {
            Thread t = new Thread(r);
            t.setName("db-saver-%d".formatted(dbSaveQueue.size()));
            t.setDaemon(true);
            return t;
        });

        for (int i = 0; i < SORTER_THREAD_POOL_SIZE; i++) {
            sorterExecutor.submit(this::sorterWorkerLoop);
        }

        for (int i = 0; i < DB_THREAD_POOL_SIZE; i++) {
            dbExecutor.submit(this::dbSaverLoop);
        }

        startMonitorThread();
        log.info("异步分拣执行器初始化完成: 分拣机线程={}, DB线程={}", SORTER_THREAD_POOL_SIZE, DB_THREAD_POOL_SIZE);
    }

    @PreDestroy
    public void shutdown() {
        running = false;
        sorterExecutor.shutdownNow();
        dbExecutor.shutdownNow();
        log.info("异步分拣执行器已关闭");
    }

    private LinkedBlockingQueue<SortingTask> getOrCreateQueue(Integer sorterId) {
        return sorterQueues.computeIfAbsent(sorterId, k -> {
            sorterLastActionTime.put(k, new AtomicLong(0));
            sorterSortCount.put(k, new AtomicLong(0));
            return new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
        });
    }

    public SortingResult submitTask(String barcode, WarehouseRoute route, Integer conveyorLine) {
        SortingResult result = new SortingResult();
        result.setBarcode(barcode);
        result.setScanTime(LocalDateTime.now());

        if (route == null) {
            result.setSuccess(false);
            result.setMessage("未找到路由信息");
            asyncSavePackageInfo(buildPackageInfo(barcode, null, conveyorLine, "ROUTE_NOT_FOUND", false));
            return result;
        }

        result.setDestinationWarehouse(route.getDestinationWarehouse());
        result.setWarehouseCode(route.getWarehouseCode());
        result.setSorterId(route.getSorterId());
        result.setConveyorLine(route.getConveyorLine() != null ? route.getConveyorLine() : conveyorLine);
        result.setSuccess(true);
        result.setMessage("分拣指令已排队");

        Integer sorterId = route.getSorterId();
        LinkedBlockingQueue<SortingTask> queue = getOrCreateQueue(sorterId);

        SortingTask task = new SortingTask(barcode, route, result.getConveyorLine(), false);

        if (!queue.offer(task)) {
            SortingTask dropped = queue.poll();
            queue.offer(task);
            log.warn("分拣机 {} 队列已满，丢弃最旧任务: {}", sorterId, dropped != null ? dropped.barcode : "null");
            totalFailed.incrementAndGet();
            if (dropped != null) {
                asyncSavePackageInfo(buildPackageInfo(dropped.barcode, dropped.route, dropped.conveyorLine, "DROPPED", false));
            }
        }

        totalProcessed.incrementAndGet();

        return result;
    }

    private void sorterWorkerLoop() {
        while (running) {
            try {
                for (Map.Entry<Integer, LinkedBlockingQueue<SortingTask>> entry : sorterQueues.entrySet()) {
                    Integer sorterId = entry.getKey();
                    LinkedBlockingQueue<SortingTask> queue = entry.getValue();

                    SortingTask task = queue.poll();
                    if (task != null) {
                        long waitMs = System.currentTimeMillis() - task.createTime;
                        totalQueueWaitMs.addAndGet(waitMs);
                        maxQueueWaitMs.updateAndGet(cur -> Math.max(cur, waitMs));

                        if (waitMs > 500) {
                            log.warn("分拣机 {} 任务等待时间过长: {}ms, 条码: {}", sorterId, waitMs, task.barcode);
                        }

                        executeSortingTask(sorterId, task);
                    }
                }
                Thread.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("分拣机工作线程异常: {}", e.getMessage(), e);
            }
        }
    }

    private void executeSortingTask(Integer sorterId, SortingTask task) {
        try {
            AtomicLong lastAction = sorterLastActionTime.get(sorterId);
            long now = System.currentTimeMillis();
            long elapsed = now - lastAction.get();

            if (elapsed < SORTER_MIN_INTERVAL_MS) {
                Thread.sleep(SORTER_MIN_INTERVAL_MS - elapsed);
            }

            SorterDevice device = routeCacheService.getSorter(sorterId);
            boolean success;
            if (device != null && device.getTcpHost() != null && device.getTcpPort() != null) {
                success = tcpClient.sendSorterCommand(device.getTcpHost(), device.getTcpPort(), sorterId);
            } else {
                success = tcpClient.sendSorterCommand(sorterId);
            }

            lastAction.set(System.currentTimeMillis());

            if (success) {
                totalSuccess.incrementAndGet();
                sorterSortCount.get(sorterId).incrementAndGet();

                SortingResult result = new SortingResult();
                result.setBarcode(task.barcode);
                result.setSorterId(sorterId);
                result.setWarehouseCode(task.route.getWarehouseCode());
                result.setDestinationWarehouse(task.route.getDestinationWarehouse());
                result.setConveyorLine(task.conveyorLine);
                result.setSuccess(true);
                result.setMessage("分拣完成");
                result.setScanTime(task.scanTime);
                result.setSortingTime(LocalDateTime.now());

                webSocketHandler.broadcastSortingEvent(result);

                PackageInfo pkg = buildPackageInfo(task.barcode, task.route, task.conveyorLine, "SORTED", true);
                asyncSavePackageInfo(pkg);
                asyncUpdateSorterStats(sorterId);

            } else {
                totalFailed.incrementAndGet();
                log.error("分拣机 {} 指令发送失败: 条码={}", sorterId, task.barcode);
                asyncSavePackageInfo(buildPackageInfo(task.barcode, task.route, task.conveyorLine, "SORT_FAILED", false));
            }

        } catch (Exception e) {
            totalFailed.incrementAndGet();
            log.error("执行分拣任务异常: 分拣机={}, 条码={}, 错误={}", sorterId, task.barcode, e.getMessage());
        }
    }

    private PackageInfo buildPackageInfo(String barcode, WarehouseRoute route, Integer conveyorLine, String status, boolean sorted) {
        PackageInfo pkg = new PackageInfo();
        pkg.setBarcode(barcode);
        pkg.setStatus(status);
        pkg.setScanTime(LocalDateTime.now());
        pkg.setConveyorLine(conveyorLine);
        if (route != null) {
            pkg.setDestinationWarehouse(route.getDestinationWarehouse());
            pkg.setWarehouseCode(route.getWarehouseCode());
            pkg.setSorterId(route.getSorterId());
        }
        if (sorted) {
            pkg.setSortingTime(LocalDateTime.now());
        }
        return pkg;
    }

    private void asyncSavePackageInfo(PackageInfo pkg) {
        if (!dbSaveQueue.offer(pkg)) {
            log.warn("数据库保存队列已满，丢弃包裹记录: {}", pkg.getBarcode());
        }
    }

    private void asyncUpdateSorterStats(Integer sorterId) {
        dbExecutor.submit(() -> {
            try {
                sorterDeviceRepository.findBySorterId(sorterId).ifPresent(device -> {
                    device.setTodaySortCount(device.getTodaySortCount() + 1);
                    device.setLastActionTime(LocalDateTime.now());
                    sorterDeviceRepository.save(device);
                });
            } catch (Exception e) {
                log.error("更新分拣机统计失败: {}", e.getMessage());
            }
        });
    }

    private void dbSaverLoop() {
        List<PackageInfo> batch = new ArrayList<>();
        while (running) {
            try {
                PackageInfo pkg = dbSaveQueue.poll(500, TimeUnit.MILLISECONDS);
                if (pkg != null) {
                    batch.add(pkg);
                }

                if (batch.size() >= 50 || (pkg == null && !batch.isEmpty())) {
                    try {
                        packageInfoRepository.saveAll(batch);
                        batch.clear();
                    } catch (Exception e) {
                        log.error("批量保存包裹记录失败: {}", e.getMessage());
                        batch.clear();
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    private void startMonitorThread() {
        Thread monitor = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(10000);
                    long queueSize = sorterQueues.values().stream().mapToInt(LinkedBlockingQueue::size).sum();
                    long avgWait = totalProcessed.get() > 0 ? totalQueueWaitMs.get() / totalProcessed.get() : 0;

                    if (queueSize > 20 || avgWait > 300) {
                        log.warn("系统负载告警: 队列积压={}, 平均等待={}ms, 最大等待={}ms",
                                queueSize, avgWait, maxQueueWaitMs.get());
                    }

                    log.info("分拣统计: 总计={}, 成功={}, 失败={}, 队列积压={}, 平均等待={}ms",
                            totalProcessed.get(), totalSuccess.get(), totalFailed.get(),
                            queueSize, avgWait);

                    maxQueueWaitMs.set(0);
                    totalQueueWaitMs.set(0);

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }, "sorting-monitor");
        monitor.setDaemon(true);
        monitor.start();
    }

    public Map<String, Object> getStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalProcessed", totalProcessed.get());
        stats.put("totalSuccess", totalSuccess.get());
        stats.put("totalFailed", totalFailed.get());
        stats.put("queueSize", sorterQueues.values().stream().mapToInt(LinkedBlockingQueue::size).sum());
        stats.put("dbQueueSize", dbSaveQueue.size());

        Map<Integer, Integer> sorterStats = new ConcurrentHashMap<>();
        sorterSortCount.forEach((id, count) -> sorterStats.put(id, count.intValue()));
        stats.put("sorterStats", sorterStats);

        Map<Integer, Integer> queueStats = new ConcurrentHashMap<>();
        sorterQueues.forEach((id, q) -> queueStats.put(id, q.size()));
        stats.put("queueStats", queueStats);

        return stats;
    }
}
