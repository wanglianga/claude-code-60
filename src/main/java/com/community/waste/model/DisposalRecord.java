package com.community.waste.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "disposal_records")
public class DisposalRecord {

    /** 垃圾四分类 */
    public enum Category {
        KITCHEN, RECYCLABLE, HAZARDOUS, OTHER
    }

    public enum Status {
        RECORDED, UNDER_REVIEW, CONFIRMED, REJECTED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bucket_point_id", nullable = false)
    private BucketPoint bucketPoint;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Category category;

    @Column(name = "weight_kg", nullable = false, precision = 8, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "photo_url", length = 512)
    private String photoUrl;

    @Column(name = "disposed_at", nullable = false)
    private OffsetDateTime disposedAt = OffsetDateTime.now();

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id")
    private AppUser supervisor;

    /** 是否破袋（厨余破袋为好习惯，加分） */
    @Column(name = "bag_broken", nullable = false)
    private boolean bagBroken = false;

    /** 是否有明显误投 */
    @Column(name = "obvious_missort", nullable = false)
    private boolean obviousMissort = false;

    /** 督导员代录（老人不会扫码） */
    @Column(name = "proxy_entry", nullable = false)
    private boolean proxyEntry = false;

    @Column(name = "points_awarded", nullable = false)
    private int pointsAwarded = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.RECORDED;

    @Column(length = 512)
    private String note;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @JsonProperty("userId")
    public Long userId() {
        return user == null ? null : user.getId();
    }

    @JsonProperty("userName")
    public String userName() {
        return user == null ? null : user.getDisplayName();
    }

    @JsonProperty("bucketPointId")
    public Long bucketPointId() {
        return bucketPoint == null ? null : bucketPoint.getId();
    }

    @JsonProperty("bucketPointName")
    public String bucketPointName() {
        return bucketPoint == null ? null : bucketPoint.getName();
    }

    @JsonProperty("supervisorName")
    public String supervisorName() {
        return supervisor == null ? null : supervisor.getDisplayName();
    }

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public BucketPoint getBucketPoint() {
        return bucketPoint;
    }

    public void setBucketPoint(BucketPoint bucketPoint) {
        this.bucketPoint = bucketPoint;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public OffsetDateTime getDisposedAt() {
        return disposedAt;
    }

    public void setDisposedAt(OffsetDateTime disposedAt) {
        this.disposedAt = disposedAt;
    }

    public AppUser getSupervisor() {
        return supervisor;
    }

    public void setSupervisor(AppUser supervisor) {
        this.supervisor = supervisor;
    }

    public boolean isBagBroken() {
        return bagBroken;
    }

    public void setBagBroken(boolean bagBroken) {
        this.bagBroken = bagBroken;
    }

    public boolean isObviousMissort() {
        return obviousMissort;
    }

    public void setObviousMissort(boolean obviousMissort) {
        this.obviousMissort = obviousMissort;
    }

    public boolean isProxyEntry() {
        return proxyEntry;
    }

    public void setProxyEntry(boolean proxyEntry) {
        this.proxyEntry = proxyEntry;
    }

    public int getPointsAwarded() {
        return pointsAwarded;
    }

    public void setPointsAwarded(int pointsAwarded) {
        this.pointsAwarded = pointsAwarded;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
