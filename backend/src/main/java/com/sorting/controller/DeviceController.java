package com.sorting.controller;

import com.sorting.entity.SorterDevice;
import com.sorting.entity.WarehouseRoute;
import com.sorting.repository.SorterDeviceRepository;
import com.sorting.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DeviceController {

    @Autowired
    private SorterDeviceRepository sorterDeviceRepository;

    @Autowired
    private RouteService routeService;

    @GetMapping("/sorters")
    public ResponseEntity<List<SorterDevice>> getAllSorters() {
        return ResponseEntity.ok(sorterDeviceRepository.findAll());
    }

    @GetMapping("/sorters/line/{lineId}")
    public ResponseEntity<List<SorterDevice>> getSortersByLine(@PathVariable Integer lineId) {
        return ResponseEntity.ok(sorterDeviceRepository.findByConveyorLine(lineId));
    }

    @GetMapping("/sorters/{sorterId}")
    public ResponseEntity<SorterDevice> getSorterById(@PathVariable Integer sorterId) {
        return sorterDeviceRepository.findBySorterId(sorterId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/routes")
    public ResponseEntity<List<WarehouseRoute>> getAllRoutes() {
        return ResponseEntity.ok(routeService.getAllActiveRoutes());
    }

    @GetMapping("/routes/line/{lineId}")
    public ResponseEntity<List<WarehouseRoute>> getRoutesByLine(@PathVariable Integer lineId) {
        return ResponseEntity.ok(routeService.getRoutesByConveyorLine(lineId));
    }
}
