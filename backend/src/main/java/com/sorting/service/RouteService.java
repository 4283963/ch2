package com.sorting.service;

import com.sorting.entity.WarehouseRoute;
import com.sorting.repository.WarehouseRouteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);

    @Autowired
    private WarehouseRouteRepository warehouseRouteRepository;

    public WarehouseRoute findRouteByBarcode(String barcode) {
        List<WarehouseRoute> routes = warehouseRouteRepository.findByBarcodePrefixMatch(barcode);
        if (routes != null && !routes.isEmpty()) {
            WarehouseRoute route = routes.get(0);
            log.info("条码 {} 匹配到路由: {} -> {}, 分拣机: {}", barcode, route.getBarcodePrefix(),
                    route.getDestinationWarehouse(), route.getSorterId());
            return route;
        }
        log.warn("条码 {} 未找到匹配的路由", barcode);
        return null;
    }

    public List<WarehouseRoute> getAllActiveRoutes() {
        return warehouseRouteRepository.findByIsActiveTrue();
    }

    public Optional<WarehouseRoute> getRouteById(Long id) {
        return warehouseRouteRepository.findById(id);
    }

    public WarehouseRoute saveRoute(WarehouseRoute route) {
        return warehouseRouteRepository.save(route);
    }

    public void deleteRoute(Long id) {
        warehouseRouteRepository.deleteById(id);
    }

    public List<WarehouseRoute> getRoutesByConveyorLine(Integer conveyorLine) {
        return warehouseRouteRepository.findByConveyorLineAndIsActiveTrue(conveyorLine);
    }
}
