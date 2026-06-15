package com.sorting.repository;

import com.sorting.entity.SorterDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SorterDeviceRepository extends JpaRepository<SorterDevice, Long> {

    Optional<SorterDevice> findBySorterId(Integer sorterId);

    List<SorterDevice> findByConveyorLine(Integer conveyorLine);

    List<SorterDevice> findByStatus(String status);

    List<SorterDevice> findByWarehouseCode(String warehouseCode);
}
