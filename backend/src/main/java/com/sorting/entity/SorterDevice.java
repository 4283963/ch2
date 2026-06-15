package com.sorting.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sorter_device")
public class SorterDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sorter_id", nullable = false, unique = true)
    private Integer sorterId;

    @Column(name = "sorter_name", nullable = false, length = 50)
    private String sorterName;

    @Column(name = "conveyor_line", nullable = false)
    private Integer conveyorLine;

    @Column(name = "position_on_line", nullable = false)
    private Integer positionOnLine;

    @Column(name = "warehouse_code", nullable = false, length = 10)
    private String warehouseCode;

    @Column(name = "warehouse_name", nullable = false, length = 50)
    private String warehouseName;

    @Column(name = "tcp_host", length = 50)
    private String tcpHost;

    @Column(name = "tcp_port")
    private Integer tcpPort;

    @Column(name = "status", length = 20)
    private String status;

    @Column(name = "today_sort_count")
    private Integer todaySortCount = 0;

    @Column(name = "last_action_time")
    private LocalDateTime lastActionTime;

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
    public Integer getSorterId() { return sorterId; }
    public void setSorterId(Integer sorterId) { this.sorterId = sorterId; }
    public String getSorterName() { return sorterName; }
    public void setSorterName(String sorterName) { this.sorterName = sorterName; }
    public Integer getConveyorLine() { return conveyorLine; }
    public void setConveyorLine(Integer conveyorLine) { this.conveyorLine = conveyorLine; }
    public Integer getPositionOnLine() { return positionOnLine; }
    public void setPositionOnLine(Integer positionOnLine) { this.positionOnLine = positionOnLine; }
    public String getWarehouseCode() { return warehouseCode; }
    public void setWarehouseCode(String warehouseCode) { this.warehouseCode = warehouseCode; }
    public String getWarehouseName() { return warehouseName; }
    public void setWarehouseName(String warehouseName) { this.warehouseName = warehouseName; }
    public String getTcpHost() { return tcpHost; }
    public void setTcpHost(String tcpHost) { this.tcpHost = tcpHost; }
    public Integer getTcpPort() { return tcpPort; }
    public void setTcpPort(Integer tcpPort) { this.tcpPort = tcpPort; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getTodaySortCount() { return todaySortCount; }
    public void setTodaySortCount(Integer todaySortCount) { this.todaySortCount = todaySortCount; }
    public LocalDateTime getLastActionTime() { return lastActionTime; }
    public void setLastActionTime(LocalDateTime lastActionTime) { this.lastActionTime = lastActionTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
