package com.community.waste.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "violations")
public class Violation {

    public enum Level {
        EDUCATION,  // 教育提醒（首次，不扣分）
        MINOR,      // 一般违规（扣分）
        MAJOR       // 严重违规
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private AppUser user;

    @Column(name = "review_task_id")
    private Long reviewTaskId;

    @Column(name = "disposal_id")
    private Long disposalId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Level level;

    @Column(name = "points_deducted", nullable = false)
    private int pointsDeducted = 0;

    @Column(length = 512)
    private String message;

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

    public Long getId() {
        return id;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    public Long getReviewTaskId() {
        return reviewTaskId;
    }

    public void setReviewTaskId(Long reviewTaskId) {
        this.reviewTaskId = reviewTaskId;
    }

    public Long getDisposalId() {
        return disposalId;
    }

    public void setDisposalId(Long disposalId) {
        this.disposalId = disposalId;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public int getPointsDeducted() {
        return pointsDeducted;
    }

    public void setPointsDeducted(int pointsDeducted) {
        this.pointsDeducted = pointsDeducted;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
