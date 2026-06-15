package com.sorting.service;

import com.sorting.websocket.SortingWebSocketHandler;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class AntiWearService {

    private static final Logger log = LoggerFactory.getLogger(AntiWearService.class);

    private static final int DEFAULT_CONSECUTIVE_THRESHOLD = 5;
    private static final long DEFAULT_IDLE_TIMEOUT_MS = 5000;
    private static final long IDLE_CHECK_INTERVAL_MS = 1000;

    @Autowired
    private SortingWebSocketHandler webSocketHandler;

    private final AtomicBoolean antiWearEnabled = new AtomicBoolean(false);

    private final Map<Integer, SorterAntiWearState> sorterStates = new ConcurrentHashMap<>();
    private final Map<Integer, Integer> conveyorLastSorter = new ConcurrentHashMap<>();

    private ScheduledExecutorService idleChecker;

    public static class AtomicString {
        private volatile String value;
        public synchronized String get() { return value; }
        public synchronized void set(String value) { this.value = value; }
    }

    public static class SorterAntiWearState {
        public final int sorterId;
        public final AtomicBoolean isHeld = new AtomicBoolean(false);
        public final AtomicString currentWarehouse = new AtomicString();
        public final AtomicInteger consecutiveCount = new AtomicInteger(0);
        public final AtomicLong lastActionTime = new AtomicLong(0);
        public final AtomicLong totalHoldSavings = new AtomicLong(0);

        public SorterAntiWearState(int sorterId) {
            this.sorterId = sorterId;
        }
    }

    @PostConstruct
    public void init() {
        idleChecker = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "antiwear-idle-checker");
            t.setDaemon(true);
            return t;
        });

        idleChecker.scheduleAtFixedRate(this::checkIdleTimeouts, IDLE_CHECK_INTERVAL_MS, IDLE_CHECK_INTERVAL_MS, TimeUnit.MILLISECONDS);

        log.info("减磨模式服务初始化完成");
    }

    @PreDestroy
    public void shutdown() {
        if (idleChecker != null) {
            idleChecker.shutdownNow();
        }
    }

    public boolean isAntiWearEnabled() {
        return antiWearEnabled.get();
    }

    public void setAntiWearEnabled(boolean enabled) {
        boolean oldValue = antiWearEnabled.getAndSet(enabled);
        if (oldValue != enabled) {
            log.info("减磨模式状态变更: {} -> {}", oldValue ? "开启" : "关闭", enabled ? "开启" : "关闭");
            if (!enabled) {
                resetAllSorters();
            }
            broadcastStatus();
        }
    }

    public int getConsecutiveThreshold() {
        return DEFAULT_CONSECUTIVE_THRESHOLD;
    }

    public long getIdleTimeoutMs() {
        return DEFAULT_IDLE_TIMEOUT_MS;
    }

    private SorterAntiWearState getOrCreateState(int sorterId) {
        return sorterStates.computeIfAbsent(sorterId, SorterAntiWearState::new);
    }

    public boolean shouldSkipSwing(int sorterId, String warehouseCode) {
        if (!antiWearEnabled.get()) {
            return false;
        }
        SorterAntiWearState state = getOrCreateState(sorterId);
        String currentWh = state.currentWarehouse.get();
        if (currentWh != null && currentWh.equals(warehouseCode) && state.isHeld.get()) {
            return true;
        }
        return false;
    }

    public SorterAntiWearState getState(int sorterId) {
        return getOrCreateState(sorterId);
    }

    public boolean recordPackage(int conveyorLine, int sorterId, String warehouseCode) {
        if (!antiWearEnabled.get()) {
            return false;
        }

        Integer lastSorter = conveyorLastSorter.get(conveyorLine);
        if (lastSorter != null && lastSorter != sorterId) {
            SorterAntiWearState lastState = sorterStates.get(lastSorter);
            if (lastState != null && lastState.isHeld.get()) {
                releaseSorter(lastSorter);
                log.info("传送带 {} 方向变更 (分拣机 {} -> {}), 复位上一分拣机挡板",
                        conveyorLine, lastSorter, sorterId);
                broadcastStatus();
            }
        }
        conveyorLastSorter.put(conveyorLine, sorterId);

        SorterAntiWearState state = getOrCreateState(sorterId);
        String currentWh = state.currentWarehouse.get();
        state.lastActionTime.set(System.currentTimeMillis());

        if (currentWh == null || !currentWh.equals(warehouseCode)) {
            if (state.isHeld.get()) {
                releaseSorter(sorterId);
                log.info("分拣机 {} 方向变更 ({} -> {}), 复位挡板", sorterId, currentWh, warehouseCode);
            }
            state.currentWarehouse.set(warehouseCode);
            state.consecutiveCount.set(1);
            return false;
        }

        int count = state.consecutiveCount.incrementAndGet();

        if (state.isHeld.get()) {
            state.totalHoldSavings.incrementAndGet();
            log.debug("分拣机 {} 保持开启中，跳过摆动 (连续第 {} 个)", sorterId, count);
            return true;
        }

        if (count >= DEFAULT_CONSECUTIVE_THRESHOLD) {
            state.isHeld.set(true);
            log.info("分拣机 {} 连续 {} 个同方向包裹，开启减磨保持模式", sorterId, count);
            broadcastStatus();
        }

        return false;
    }

    private void releaseSorter(int sorterId) {
        SorterAntiWearState state = sorterStates.get(sorterId);
        if (state != null) {
            state.isHeld.set(false);
            state.consecutiveCount.set(0);
            state.currentWarehouse.set(null);
            log.info("分拣机 {} 挡板复位", sorterId);
        }
    }

    private void checkIdleTimeouts() {
        if (!antiWearEnabled.get()) {
            return;
        }

        long now = System.currentTimeMillis();
        boolean changed = false;
        for (Map.Entry<Integer, SorterAntiWearState> entry : sorterStates.entrySet()) {
            SorterAntiWearState state = entry.getValue();
            if (state.isHeld.get() && (now - state.lastActionTime.get()) > DEFAULT_IDLE_TIMEOUT_MS) {
                releaseSorter(entry.getKey());
                log.info("分拣机 {} 空闲超时 ({}ms)，自动复位挡板", entry.getKey(), now - state.lastActionTime.get());
                changed = true;
            }
        }
        if (changed) {
            broadcastStatus();
        }
    }

    private void resetAllSorters() {
        for (Integer sorterId : sorterStates.keySet()) {
            releaseSorter(sorterId);
        }
        conveyorLastSorter.clear();
        log.info("所有分拣机挡板已全部复位");
    }

    public Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("enabled", antiWearEnabled.get());
        status.put("consecutiveThreshold", DEFAULT_CONSECUTIVE_THRESHOLD);
        status.put("idleTimeoutMs", DEFAULT_IDLE_TIMEOUT_MS);

        Map<String, Map<String, Object>> sorterStatusMap = new HashMap<>();
        for (Map.Entry<Integer, SorterAntiWearState> entry : sorterStates.entrySet()) {
            SorterAntiWearState state = entry.getValue();
            Map<String, Object> s = new HashMap<>();
            s.put("isHeld", state.isHeld.get());
            s.put("currentWarehouse", state.currentWarehouse.get());
            s.put("consecutiveCount", state.consecutiveCount.get());
            s.put("totalHoldSavings", state.totalHoldSavings.get());
            s.put("lastActionTime", state.lastActionTime.get());
            sorterStatusMap.put(String.valueOf(entry.getKey()), s);
        }
        status.put("sorters", sorterStatusMap);

        return status;
    }

    private void broadcastStatus() {
        Map<String, Object> msg = new ConcurrentHashMap<>();
        msg.put("type", "ANTI_WEAR_STATUS");
        msg.put("data", getStatus());
        webSocketHandler.broadcastMessage(JSON.toJSONString(msg));
    }

    public void triggerBroadcast() {
        broadcastStatus();
    }
}
