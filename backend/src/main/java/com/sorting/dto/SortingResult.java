package com.sorting.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class SortingResult implements Serializable {

    private static final long serialVersionUID = 1L;

    private String barcode;
    private String destinationWarehouse;
    private String warehouseCode;
    private Integer sorterId;
    private Integer conveyorLine;
    private Boolean success;
    private String message;
    private LocalDateTime scanTime;
    private LocalDateTime sortingTime;

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getDestinationWarehouse() { return destinationWarehouse; }
    public void setDestinationWarehouse(String destinationWarehouse) { this.destinationWarehouse = destinationWarehouse; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public Integer getSorterId() { return sorterId; }
    public void setSorterId(Integer sorterId) { this.sorterId = sorterId; }
    public Integer getConveyorLine() { return conveyorLine; }
    public void setConveyorLine(Integer conveyorLine) { this.conveyorLine = conveyorLine; }
    public Boolean getSuccess() { return success; }
    public void setSuccess(Boolean success) { this.success = success; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public LocalDateTime getScanTime() { return scanTime; }
    public void setScanTime(LocalDateTime scanTime) { this.scanTime = scanTime; }
    public LocalDateTime getSortingTime() { return sortingTime; }
    public void setSortingTime(LocalDateTime sortingTime) { this.sortingTime = sortingTime; }
}
