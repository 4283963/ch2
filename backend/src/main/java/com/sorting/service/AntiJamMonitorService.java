package com.sorting.service;

import com.sorting.tcp.AsyncSortingExecutor;
import com.sorting.websocket.SortingWebSocketHandler;
import com.alibaba.fastjson.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class AntiJamMonitorService {

    private static final Logger log = LoggerFactory.getLogger(AntiJamMonitorService.class);

    private static final long LATENCY_WARN_THRESHOLD_MS = 200;
    private static final long LATENCY_ALARM_THRESHOLD_MS = 500;
    private static final int QUEUE_WARN_THRESHOLD = 10;
    private static final int QUEUE_ALARM_THRESHOLD = 30;
    private static final int QPS_WARN_THRESHOLD = 50;
    private static final int QPS_ALARM_THRESHOLD = 100;
    private static final int HISTORY_SIZE = 60;

    @Autowired
    private AsyncSortingExecutor asyncSortingExecutor;

    @Autowired
    private SortingWebSocketHandler webSocketHandler;

    private final ConcurrentLinkedDeque<QpsSample> qpsHistory = new ConcurrentLinkedDeque<>();
    private final AtomicLong lastTotalProcessed = new AtomicLong(0);
    private final AtomicInteger currentQps = new AtomicInteger(0);
    private final AtomicLong maxLatency = new AtomicLong(0);
    private final AtomicLong avgLatency = new AtomicLong(0);
    private volatile String systemStatus = "NORMAL";

    private final List<AlarmListener> alarmListeners = new ArrayList<>();

    public static class QpsSample {
        long timestamp;
        int qps;
        long queueSize;
        long avgLatency;
        String status;

        public QpsSample(long timestamp, int qps, long queueSize, long avgLatency, String status) {
            this.timestamp = timestamp;
            this.qps = qps;
            this.queueSize = queueSize;
            this.avgLatency = avgLatency;
            this.status = status;
        }
    }

    public interface AlarmListener {
        void onAlarm(String level, String message, Map<String, Object> data);
    }

    @Scheduled(fixedRate = 1000)
    public void monitorSystem() {
        try {
            Map<String, Object> stats = asyncSortingExecutor.getStats();
            long totalProcessed = ((Number) stats.get("totalProcessed")).longValue();
            long queueSize = ((Number) stats.get("queueSize")).longValue();
            long dbQueueSize = ((Number) stats.get("dbQueueSize")).longValue();

            int qps = (int) (totalProcessed - lastTotalProcessed.get());
            currentQps.set(qps);
            lastTotalProcessed.set(totalProcessed);

            Map<String, Object> health = new LinkedHashMap<>();
            health.put("timestamp", System.currentTimeMillis());
            health.put("qps", qps);
            health.put("queueSize", queueSize);
            health.put("dbQueueSize", dbQueueSize);
            health.put("totalProcessed", totalProcessed);
            health.put("totalSuccess", stats.get("totalSuccess"));
            health.put("totalFailed", stats.get("totalFailed"));
            health.put("queueStats", stats.get("queueStats"));
            health.put("sorterStats", stats.get("sorterStats"));

            String newStatus = evaluateStatus(qps, queueSize, avgLatency.get());
            health.put("status", newStatus);
            health.put("maxLatency", maxLatency.get());
            health.put("avgLatency", avgLatency.get());

            if (!systemStatus.equals(newStatus)) {
                String level = newStatus.equals("NORMAL") ? "INFO" : (newStatus.equals("WARN") ? "WARN" : "ALARM");
                String message = String.format("系统状态变更: %s -> %s (QPS=%d, 队列=%d)", systemStatus, newStatus, qps, queueSize);
                triggerAlarm(level, message, health);
                systemStatus = newStatus;
            }

            qpsHistory.add(new QpsSample(System.currentTimeMillis(), qps, queueSize, avgLatency.get(), newStatus));
            while (qpsHistory.size() > HISTORY_SIZE) {
                qpsHistory.poll();
            }

            if (!newStatus.equals("NORMAL")) {
                log.warn("{} - QPS: {}, 队列: {}, 平均延迟: {}ms", newStatus, qps, queueSize, avgLatency.get());
            }

            Map<String, Object> monitorMsg = new LinkedHashMap<>();
            monitorMsg.put("type", "MONITOR");
            monitorMsg.put("data", health);
            webSocketHandler.broadcastMessage(JSON.toJSONString(monitorMsg));

        } catch (Exception e) {
            log.error("系统监控异常: {}", e.getMessage(), e);
        }
    }

    private String evaluateStatus(int qps, long queueSize, long latency) {
        boolean alarm = (queueSize >= QUEUE_ALARM_THRESHOLD) ||
                (latency >= LATENCY_ALARM_THRESHOLD_MS) ||
                (qps >= QPS_ALARM_THRESHOLD && queueSize > QUEUE_WARN_THRESHOLD);

        if (alarm) {
            return "ALARM";
        }

        boolean warn = (queueSize >= QUEUE_WARN_THRESHOLD) ||
                (latency >= LATENCY_WARN_THRESHOLD_MS) ||
                (qps >= QPS_WARN_THRESHOLD);

        if (warn) {
            return "WARN";
        }

        return "NORMAL";
    }

    private void triggerAlarm(String level, String message, Map<String, Object> data) {
        for (AlarmListener listener : alarmListeners) {
            try {
                listener.onAlarm(level, message, data);
            } catch (Exception e) {
                log.error("告警监听器异常: {}", e.getMessage());
            }
        }

        if ("ALARM".equals(level)) {
            log.error("【严重告警】{}", message);
        } else if ("WARN".equals(level)) {
            log.warn("【警告】{}", message);
        } else {
            log.info("【系统正常】{}", message);
        }
    }

    public void recordLatency(long latencyMs) {
        maxLatency.updateAndGet(cur -> Math.max(cur, latencyMs));
        avgLatency.updateAndGet(cur -> (cur * 9 + latencyMs) / 10);
    }

    public void addAlarmListener(AlarmListener listener) {
        alarmListeners.add(listener);
    }

    public Map<String, Object> getCurrentStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("systemStatus", systemStatus);
        status.put("currentQps", currentQps.get());
        status.put("maxLatency", maxLatency.get());
        status.put("avgLatency", avgLatency.get());
        status.put("qpsHistory", getQpsHistoryList());
        status.put("latencyWarnThreshold", LATENCY_WARN_THRESHOLD_MS);
        status.put("latencyAlarmThreshold", LATENCY_ALARM_THRESHOLD_MS);
        status.put("queueWarnThreshold", QUEUE_WARN_THRESHOLD);
        status.put("queueAlarmThreshold", QUEUE_ALARM_THRESHOLD);
        return status;
    }

    private List<Map<String, Object>> getQpsHistoryList() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (QpsSample sample : qpsHistory) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("timestamp", sample.timestamp);
            m.put("qps", sample.qps);
            m.put("queueSize", sample.queueSize);
            m.put("avgLatency", sample.avgLatency);
            m.put("status", sample.status);
            list.add(m);
        }
        return list;
    }

    public void resetStats() {
        lastTotalProcessed.set(0);
        currentQps.set(0);
        maxLatency.set(0);
        avgLatency.set(0);
        qpsHistory.clear();
        systemStatus = "NORMAL";
    }

    public String getSystemStatus() {
        return systemStatus;
    }
}
