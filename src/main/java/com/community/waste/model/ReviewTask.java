package com.community.waste.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "review_tasks")
public class ReviewTask {

    /** 误投问题类型 */
    public enum IssueType {
        PLASTIC_IN_KITCHEN,       // 塑料袋混入厨余
        BATTERY_IN_OTHER,         // 电池混入其他垃圾
        CARDBOARD_NOT_FLATTENED,  // 纸箱未压扁
        CONTAINER_NOT_CLEANED,    // 餐盒未清洗
        OBVIOUS_MISSORT           // 其他明显误投
    }

    public enum Source {
        CAMERA, SUPERVISOR
    }

    public enum Status {
        PENDING, CONFIRMED, REJECTED
    }

    /** 复核处理动作 */
    public enum Action {
        NONE, EDUCATION, DEDUCT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "disposal_id", nullable = false)
    private DisposalRecord disposal;

    @Enumerated(EnumType.STRING)
    @Column(name = "issue_type", nullable = false, length = 40)
    private IssueType issueType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Source source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.PENDING;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private AppUser assignee;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "due_at", nullable = false)
    private OffsetDateTime dueAt;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private AppUser reviewer;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private Action action;

    @Column(name = "deduct_points", nullable = false)
    private int deductPoints = 0;

    @Column(length = 512)
    private String note;

    @JsonProperty("disposalId")
    public Long disposalId() {
        return disposal == null ? null : disposal.getId();
    }

    @JsonProperty("assigneeName")
    public String assigneeName() {
        return assignee == null ? null : assignee.getDisplayName();
    }

    @JsonProperty("reviewerName")
    public String reviewerName() {
        return reviewer == null ? null : reviewer.getDisplayName();
    }

    public Long getId() {
        return id;
    }

    @JsonProperty("disposal")
    public DisposalRecord getDisposal() {
        return disposal;
    }

    public void setDisposal(DisposalRecord disposal) {
        this.disposal = disposal;
    }

    public IssueType getIssueType() {
        return issueType;
    }

    public void setIssueType(IssueType issueType) {
        this.issueType = issueType;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public AppUser getAssignee() {
        return assignee;
    }

    public void setAssignee(AppUser assignee) {
        this.assignee = assignee;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(OffsetDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public OffsetDateTime getReviewedAt() {
        return reviewedAt;
    }

    public void setReviewedAt(OffsetDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }

    public AppUser getReviewer() {
        return reviewer;
    }

    public void setReviewer(AppUser reviewer) {
        this.reviewer = reviewer;
    }

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    public int getDeductPoints() {
        return deductPoints;
    }

    public void setDeductPoints(int deductPoints) {
        this.deductPoints = deductPoints;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
