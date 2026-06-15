package com.sorting.dto;

import java.io.Serializable;

public class ScanRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private String barcode;
    private String scannerId;
    private Integer conveyorLine;
    private Long timestamp;

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }
    public String getScannerId() { return scannerId; }
    public void setScannerId(String scannerId) { this.scannerId = scannerId; }
    public Integer getConveyorLine() { return conveyorLine; }
    public void setConveyorLine(Integer conveyorLine) { this.conveyorLine = conveyorLine; }
    public Long getTimestamp() { return timestamp; }
    public void setTimestamp(Long timestamp) { this.timestamp = timestamp; }
}
