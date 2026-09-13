package com.community.waste.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "collection_records")
public class CollectionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bucket_point_id", nullable = false)
    private BucketPoint bucketPoint;

    @Column(name = "truck_no", nullable = false, length = 32)
    private String truckNo;

    @Column(nullable = false, length = 128)
    private String company = "城绿清运公司";

    @Column(name = "scheduled_at", nullable = false)
    private OffsetDateTime scheduledAt;

    @Column(name = "arrived_at")
    private OffsetDateTime arrivedAt;

    @Column(name = "weight_kg", precision = 10, scale = 2)
    private BigDecimal weightKg;

    /** 根据投放记录估算的应收重量 */
    @Column(name = "expected_weight_kg", precision = 10, scale = 2)
    private BigDecimal expectedWeightKg;

    @Column(nullable = false)
    private boolean anomaly = false;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private AppUser createdBy;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @JsonProperty("bucketPointId")
    public Long bucketPointId() {
        return bucketPoint == null ? null : bucketPoint.getId();
    }

    @JsonProperty("bucketPointName")
    public String bucketPointName() {
        return bucketPoint == null ? null : bucketPoint.getName();
    }

    public Long getId() {
        return id;
    }

    public BucketPoint getBucketPoint() {
        return bucketPoint;
    }

    public void setBucketPoint(BucketPoint bucketPoint) {
        this.bucketPoint = bucketPoint;
    }

    public String getTruckNo() {
        return truckNo;
    }

    public void setTruckNo(String truckNo) {
        this.truckNo = truckNo;
    }

    public String getCompany() {
        return company;
    }

    public void setCompany(String company) {
        this.company = company;
    }

    public OffsetDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(OffsetDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public OffsetDateTime getArrivedAt() {
        return arrivedAt;
    }

    public void setArrivedAt(OffsetDateTime arrivedAt) {
        this.arrivedAt = arrivedAt;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public BigDecimal getExpectedWeightKg() {
        return expectedWeightKg;
    }

    public void setExpectedWeightKg(BigDecimal expectedWeightKg) {
        this.expectedWeightKg = expectedWeightKg;
    }

    public boolean isAnomaly() {
        return anomaly;
    }

    public void setAnomaly(boolean anomaly) {
        this.anomaly = anomaly;
    }

    public AppUser getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(AppUser createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
