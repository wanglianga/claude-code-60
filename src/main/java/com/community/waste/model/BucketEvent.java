package com.community.waste.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "bucket_events")
public class BucketEvent {

    public enum Type {
        MISSORT_RATE_HIGH,  // 楼栋误投率升高
        REVIEW_SLOW,        // 督导员复核过慢
        OVERFLOW,           // 桶点满溢
        TRUCK_LATE,         // 清运车未按时到达
        APPEAL_FILED,       // 居民申诉扣分
        WEIGH_ANOMALY       // 清运称重异常
    }

    public enum Status {
        OPEN, ACKNOWLEDGED, RESOLVED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bucket_point_id")
    private BucketPoint bucketPoint;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id")
    private Building building;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.OPEN;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 1024)
    private String detail;

    @Column(name = "dedupe_key", length = 255)
    private String dedupeKey;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "resolved_at")
    private OffsetDateTime resolvedAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by_id")
    private AppUser resolvedBy;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<EventParticipant> participants = new ArrayList<>();

    @JsonProperty("bucketPointId")
    public Long bucketPointId() {
        return bucketPoint == null ? null : bucketPoint.getId();
    }

    @JsonProperty("bucketPointName")
    public String bucketPointName() {
        return bucketPoint == null ? null : bucketPoint.getName();
    }

    @JsonProperty("buildingName")
    public String buildingName() {
        return building == null ? null : building.getName();
    }

    @JsonProperty("resolvedByName")
    public String resolvedByName() {
        return resolvedBy == null ? null : resolvedBy.getDisplayName();
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

    public Building getBuilding() {
        return building;
    }

    public void setBuilding(Building building) {
        this.building = building;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getDedupeKey() {
        return dedupeKey;
    }

    public void setDedupeKey(String dedupeKey) {
        this.dedupeKey = dedupeKey;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(OffsetDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public AppUser getResolvedBy() {
        return resolvedBy;
    }

    public void setResolvedBy(AppUser resolvedBy) {
        this.resolvedBy = resolvedBy;
    }

    public List<EventParticipant> getParticipants() {
        return participants;
    }

    public void setParticipants(List<EventParticipant> participants) {
        this.participants = participants;
    }
}
