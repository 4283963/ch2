package com.sorting.repository;

import com.sorting.entity.WarehouseRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseRouteRepository extends JpaRepository<WarehouseRoute, Long> {

    @Query("SELECT wr FROM WarehouseRoute wr WHERE wr.isActive = true AND :barcode LIKE CONCAT(wr.barcodePrefix, '%') ORDER BY LENGTH(wr.barcodePrefix) DESC")
    List<WarehouseRoute> findByBarcodePrefixMatch(@Param("barcode") String barcode);

    Optional<WarehouseRoute> findByBarcodePrefixAndIsActiveTrue(String barcodePrefix);

    List<WarehouseRoute> findByDestinationWarehouseAndIsActiveTrue(String destinationWarehouse);

    List<WarehouseRoute> findByConveyorLineAndIsActiveTrue(Integer conveyorLine);

    List<WarehouseRoute> findByIsActiveTrue();
}
