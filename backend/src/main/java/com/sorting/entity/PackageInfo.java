package com.sorting.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "package_info")
public class PackageInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "barcode", nullable = false, length = 50, unique = true)
    private String barcode;

    @Column(name = "weight")
    private Double weight;

    @Column(name = "destination_warehouse", length = 50)
    private String destinationWarehouse;

    @Column(name = "warehouse_code", length = 10)
    private String warehouseCode;

    @Column(name = "sorter_id")
    private Integer sorterId;

    @Column(name = "conveyor_line")
    private Integer conveyorLine;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "scan_time")
    private LocalDateTime scanTime;

    @Column(name = "sorting_time")
    private LocalDateTime sortingTime;

    @Column(name = "create_time")
    private LocalDateTime createTime;

    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    protected void onCreate() {
        createTime = LocalDateTime.now();
        updateTime = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    public String getDestinationWarehouse() { return destinationWarehouse; }
    public void setDestinationWarehouse(String destinationWarehouse) { this.destinationWarehouse = destinationWarehouse; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public Integer getSorterId() { return sorterId; }
    public void setSorterId(Integer sorterId) { this.sorterId = sorterId; }
    public Integer getConveyorLine() { return conveyorLine; }
    public void setConveyorLine(Integer conveyorLine) { this.conveyorLine = conveyorLine; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getScanTime() { return scanTime; }
    public void setScanTime(LocalDateTime scanTime) { this.scanTime = scanTime; }
    public LocalDateTime getSortingTime() { return sortingTime; }
    public void setSortingTime(LocalDateTime sortingTime) { this.sortingTime = sortingTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
