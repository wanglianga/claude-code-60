package com.community.waste.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "bucket_points")
public class BucketPoint {

    public enum Status {
        ACTIVE, MERGED, CLOSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 32)
    private String code;

    @Column(nullable = false, length = 128)
    private String name;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private Building building;

    @Column(length = 255)
    private String address;

    /** 定时投放窗口开始，格式 HH:mm */
    @Column(name = "open_start", nullable = false, length = 8)
    private String openStart = "06:30";

    /** 定时投放窗口结束，格式 HH:mm */
    @Column(name = "open_end", nullable = false, length = 8)
    private String openEnd = "21:00";

    @Column(name = "capacity_kg", nullable = false, precision = 10, scale = 2)
    private BigDecimal capacityKg = new BigDecimal("200");

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.ACTIVE;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merged_into")
    private BucketPoint mergedInto;

    @Column(name = "merged_at")
    private OffsetDateTime mergedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @JsonProperty("buildingId")
    public Long buildingId() {
        return building == null ? null : building.getId();
    }

    @JsonProperty("buildingName")
    public String buildingName() {
        return building == null ? null : building.getName();
    }

    @JsonProperty("mergedIntoId")
    public Long mergedIntoId() {
        return mergedInto == null ? null : mergedInto.getId();
    }

    public Long getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Building getBuilding() {
        return building;
    }

    public void setBuilding(Building building) {
        this.building = building;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getOpenStart() {
        return openStart;
    }

    public void setOpenStart(String openStart) {
        this.openStart = openStart;
    }

    public String getOpenEnd() {
        return openEnd;
    }

    public void setOpenEnd(String openEnd) {
        this.openEnd = openEnd;
    }

    public BigDecimal getCapacityKg() {
        return capacityKg;
    }

    public void setCapacityKg(BigDecimal capacityKg) {
        this.capacityKg = capacityKg;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public BucketPoint getMergedInto() {
        return mergedInto;
    }

    public void setMergedInto(BucketPoint mergedInto) {
        this.mergedInto = mergedInto;
    }

    public OffsetDateTime getMergedAt() {
        return mergedAt;
    }

    public void setMergedAt(OffsetDateTime mergedAt) {
        this.mergedAt = mergedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
