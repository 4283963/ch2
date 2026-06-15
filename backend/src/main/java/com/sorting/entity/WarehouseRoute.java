package com.sorting.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "warehouse_route")
public class WarehouseRoute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "barcode_prefix", nullable = false, length = 20)
    private String barcodePrefix;

    @Column(name = "destination_warehouse", nullable = false, length = 50)
    private String destinationWarehouse;

    @Column(name = "warehouse_code", nullable = false, length = 10)
    private String warehouseCode;

    @Column(name = "sorter_id", nullable = false)
    private Integer sorterId;

    @Column(name = "sorter_position")
    private Integer sorterPosition;

    @Column(name = "conveyor_line", nullable = false)
    private Integer conveyorLine;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

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
    public String getBarcodePrefix() { return barcodePrefix; }
    public void setBarcodePrefix(String barcodePrefix) { this.barcodePrefix = barcodePrefix; }
    public String getDestinationWarehouse() { return destinationWarehouse; }
    public void setDestinationWarehouse(String destinationWarehouse) { this.destinationWarehouse = destinationWarehouse; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public Integer getSorterId() { return sorterId; }
    public void setSorterId(Integer sorterId) { this.sorterId = sorterId; }
    public Integer getSorterPosition() { return sorterPosition; }
    public void setSorterPosition(Integer sorterPosition) { this.sorterPosition = sorterPosition; }
    public Integer getConveyorLine() { return conveyorLine; }
    public void setConveyorLine(Integer conveyorLine) { this.conveyorLine = conveyorLine; }
    public Boolean getIsActive() { return isActive; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
