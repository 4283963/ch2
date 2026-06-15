package com.sorting.config;

import com.sorting.entity.SorterDevice;
import com.sorting.entity.WarehouseRoute;
import com.sorting.repository.SorterDeviceRepository;
import com.sorting.repository.WarehouseRouteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    @Autowired
    private WarehouseRouteRepository warehouseRouteRepository;

    @Autowired
    private SorterDeviceRepository sorterDeviceRepository;

    @Override
    public void run(String... args) throws Exception {
        if (warehouseRouteRepository.count() == 0) {
            initWarehouseRoutes();
        }
        if (sorterDeviceRepository.count() == 0) {
            initSorterDevices();
        }
        log.info("数据初始化完成");
    }

    private void initWarehouseRoutes() {
        WarehouseRoute route1 = new WarehouseRoute();
        route1.setBarcodePrefix("BJ");
        route1.setDestinationWarehouse("北京仓");
        route1.setWarehouseCode("BJ01");
        route1.setSorterId(101);
        route1.setSorterPosition(1);
        route1.setConveyorLine(1);
        route1.setIsActive(true);
        warehouseRouteRepository.save(route1);

        WarehouseRoute route2 = new WarehouseRoute();
        route2.setBarcodePrefix("SH");
        route2.setDestinationWarehouse("上海仓");
        route2.setWarehouseCode("SH01");
        route2.setSorterId(102);
        route2.setSorterPosition(2);
        route2.setConveyorLine(1);
        route2.setIsActive(true);
        warehouseRouteRepository.save(route2);

        WarehouseRoute route3 = new WarehouseRoute();
        route3.setBarcodePrefix("GZ");
        route3.setDestinationWarehouse("广州仓");
        route3.setWarehouseCode("GZ01");
        route3.setSorterId(103);
        route3.setSorterPosition(3);
        route3.setConveyorLine(1);
        route3.setIsActive(true);
        warehouseRouteRepository.save(route3);

        WarehouseRoute route4 = new WarehouseRoute();
        route4.setBarcodePrefix("SZ");
        route4.setDestinationWarehouse("深圳仓");
        route4.setWarehouseCode("SZ01");
        route4.setSorterId(104);
        route4.setSorterPosition(4);
        route4.setConveyorLine(1);
        route4.setIsActive(true);
        warehouseRouteRepository.save(route4);

        WarehouseRoute route5 = new WarehouseRoute();
        route5.setBarcodePrefix("CD");
        route5.setDestinationWarehouse("成都仓");
        route5.setWarehouseCode("CD01");
        route5.setSorterId(201);
        route5.setSorterPosition(1);
        route5.setConveyorLine(2);
        route5.setIsActive(true);
        warehouseRouteRepository.save(route5);

        WarehouseRoute route6 = new WarehouseRoute();
        route6.setBarcodePrefix("WH");
        route6.setDestinationWarehouse("武汉仓");
        route6.setWarehouseCode("WH01");
        route6.setSorterId(202);
        route6.setSorterPosition(2);
        route6.setConveyorLine(2);
        route6.setIsActive(true);
        warehouseRouteRepository.save(route6);

        WarehouseRoute route7 = new WarehouseRoute();
        route7.setBarcodePrefix("XA");
        route7.setDestinationWarehouse("西安仓");
        route7.setWarehouseCode("XA01");
        route7.setSorterId(203);
        route7.setSorterPosition(3);
        route7.setConveyorLine(2);
        route7.setIsActive(true);
        warehouseRouteRepository.save(route7);

        log.info("初始化了 7 条路由规则");
    }

    private void initSorterDevices() {
        SorterDevice s1 = new SorterDevice();
        s1.setSorterId(101);
        s1.setSorterName("1号线-北京仓分拣机");
        s1.setConveyorLine(1);
        s1.setPositionOnLine(1);
        s1.setWarehouseCode("BJ01");
        s1.setWarehouseName("北京仓");
        s1.setStatus("ONLINE");
        s1.setTodaySortCount(0);
        sorterDeviceRepository.save(s1);

        SorterDevice s2 = new SorterDevice();
        s2.setSorterId(102);
        s2.setSorterName("1号线-上海仓分拣机");
        s2.setConveyorLine(1);
        s2.setPositionOnLine(2);
        s2.setWarehouseCode("SH01");
        s2.setWarehouseName("上海仓");
        s2.setStatus("ONLINE");
        s2.setTodaySortCount(0);
        sorterDeviceRepository.save(s2);

        SorterDevice s3 = new SorterDevice();
        s3.setSorterId(103);
        s3.setSorterName("1号线-广州仓分拣机");
        s3.setConveyorLine(1);
        s3.setPositionOnLine(3);
        s3.setWarehouseCode("GZ01");
        s3.setWarehouseName("广州仓");
        s3.setStatus("ONLINE");
        s3.setTodaySortCount(0);
        sorterDeviceRepository.save(s3);

        SorterDevice s4 = new SorterDevice();
        s4.setSorterId(104);
        s4.setSorterName("1号线-深圳仓分拣机");
        s4.setConveyorLine(1);
        s4.setPositionOnLine(4);
        s4.setWarehouseCode("SZ01");
        s4.setWarehouseName("深圳仓");
        s4.setStatus("ONLINE");
        s4.setTodaySortCount(0);
        sorterDeviceRepository.save(s4);

        SorterDevice s5 = new SorterDevice();
        s5.setSorterId(201);
        s5.setSorterName("2号线-成都仓分拣机");
        s5.setConveyorLine(2);
        s5.setPositionOnLine(1);
        s5.setWarehouseCode("CD01");
        s5.setWarehouseName("成都仓");
        s5.setStatus("ONLINE");
        s5.setTodaySortCount(0);
        sorterDeviceRepository.save(s5);

        SorterDevice s6 = new SorterDevice();
        s6.setSorterId(202);
        s6.setSorterName("2号线-武汉仓分拣机");
        s6.setConveyorLine(2);
        s6.setPositionOnLine(2);
        s6.setWarehouseCode("WH01");
        s6.setWarehouseName("武汉仓");
        s6.setStatus("ONLINE");
        s6.setTodaySortCount(0);
        sorterDeviceRepository.save(s6);

        SorterDevice s7 = new SorterDevice();
        s7.setSorterId(203);
        s7.setSorterName("2号线-西安仓分拣机");
        s7.setConveyorLine(2);
        s7.setPositionOnLine(3);
        s7.setWarehouseCode("XA01");
        s7.setWarehouseName("西安仓");
        s7.setStatus("ONLINE");
        s7.setTodaySortCount(0);
        sorterDeviceRepository.save(s7);

        log.info("初始化了 7 台分拣设备");
    }
}
