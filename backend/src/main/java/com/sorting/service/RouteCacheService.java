package com.sorting.service;

import com.sorting.entity.SorterDevice;
import com.sorting.entity.WarehouseRoute;
import com.sorting.repository.SorterDeviceRepository;
import com.sorting.repository.WarehouseRouteRepository;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class RouteCacheService {

    private static final Logger log = LoggerFactory.getLogger(RouteCacheService.class);

    @Autowired
    private WarehouseRouteRepository warehouseRouteRepository;

    @Autowired
    private SorterDeviceRepository sorterDeviceRepository;

    private final List<WarehouseRoute> sortedRoutes = new ArrayList<>();
    private final Map<Integer, SorterDevice> sorterCache = new ConcurrentHashMap<>();
    private final Map<String, WarehouseRoute> prefixCache = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        refreshCache();
        log.info("路由缓存初始化完成: {} 条路由, {} 台分拣机", sortedRoutes.size(), sorterCache.size());
    }

    @Scheduled(fixedRate = 30000)
    public void refreshCache() {
        try {
            List<WarehouseRoute> routes = warehouseRouteRepository.findByIsActiveTrue();
            List<WarehouseRoute> sorted = routes.stream()
                    .sorted((a, b) -> Integer.compare(b.getBarcodePrefix().length(), a.getBarcodePrefix().length()))
                    .collect(Collectors.toList());

            List<SorterDevice> sorters = sorterDeviceRepository.findAll();
            Map<Integer, SorterDevice> sorterMap = new ConcurrentHashMap<>();
            Map<String, WarehouseRoute> prefixMap = new ConcurrentHashMap<>();

            for (SorterDevice s : sorters) {
                sorterMap.put(s.getSorterId(), s);
            }
            for (WarehouseRoute r : sorted) {
                prefixMap.put(r.getBarcodePrefix(), r);
            }

            sortedRoutes.clear();
            sortedRoutes.addAll(sorted);
            sorterCache.clear();
            sorterCache.putAll(sorterMap);
            prefixCache.clear();
            prefixCache.putAll(prefixMap);

            log.debug("路由缓存刷新完成: {} 条路由, {} 台分拣机", sortedRoutes.size(), sorterCache.size());
        } catch (Exception e) {
            log.error("刷新路由缓存失败: {}", e.getMessage(), e);
        }
    }

    public WarehouseRoute findRouteByBarcode(String barcode) {
        WarehouseRoute directMatch = prefixCache.get(barcode);
        if (directMatch != null) {
            return directMatch;
        }

        for (WarehouseRoute route : sortedRoutes) {
            if (barcode.startsWith(route.getBarcodePrefix())) {
                return route;
            }
        }
        return null;
    }

    public SorterDevice getSorter(Integer sorterId) {
        return sorterCache.get(sorterId);
    }

    public List<WarehouseRoute> getAllRoutes() {
        return new ArrayList<>(sortedRoutes);
    }

    public Collection<SorterDevice> getAllSorters() {
        return sorterCache.values();
    }

    public int getRouteCount() {
        return sortedRoutes.size();
    }

    public int getSorterCount() {
        return sorterCache.size();
    }
}
