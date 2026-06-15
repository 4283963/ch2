package com.sorting.repository;

import com.sorting.entity.PackageInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PackageInfoRepository extends JpaRepository<PackageInfo, Long> {

    Optional<PackageInfo> findByBarcode(String barcode);

    List<PackageInfo> findByStatus(String status);

    List<PackageInfo> findByScanTimeAfter(LocalDateTime time);

    @Query("SELECT p.destinationWarehouse, COUNT(p) FROM PackageInfo p WHERE p.scanTime >= :startTime GROUP BY p.destinationWarehouse")
    List<Object[]> countByDestinationWarehouse(LocalDateTime startTime);

    @Query("SELECT COUNT(p) FROM PackageInfo p WHERE p.scanTime >= :startTime")
    Long countTodayScanned(LocalDateTime startTime);

    @Query("SELECT COUNT(p) FROM PackageInfo p WHERE p.status = 'SORTED' AND p.sortingTime >= :startTime")
    Long countTodaySorted(LocalDateTime startTime);
}
