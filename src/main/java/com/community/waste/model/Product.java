package com.community.waste.model;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "products")
public class Product {

    public enum Category {
        GOODS,   // 实物：米面油、垃圾袋
        SERVICE  // 社区服务
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 128)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Category category = Category.GOODS;

    @Column(name = "cost_points", nullable = false)
    private int costPoints;

    @Column(nullable = false)
    private int stock = 0;

    @Column(name = "per_user_monthly_limit", nullable = false)
    private int perUserMonthlyLimit = 5;

    @Column(name = "per_family_monthly_limit", nullable = false)
    private int perFamilyMonthlyLimit = 10;

    @Column(name = "valid_from", nullable = false)
    private OffsetDateTime validFrom = OffsetDateTime.now();

    /** 过期时间，NULL 为长期有效 */
    @Column(name = "valid_to")
    private OffsetDateTime validTo;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Transient
    public boolean isExpired() {
        return validTo != null && OffsetDateTime.now().isAfter(validTo);
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public int getCostPoints() {
        return costPoints;
    }

    public void setCostPoints(int costPoints) {
        this.costPoints = costPoints;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
    }

    public int getPerUserMonthlyLimit() {
        return perUserMonthlyLimit;
    }

    public void setPerUserMonthlyLimit(int perUserMonthlyLimit) {
        this.perUserMonthlyLimit = perUserMonthlyLimit;
    }

    public int getPerFamilyMonthlyLimit() {
        return perFamilyMonthlyLimit;
    }

    public void setPerFamilyMonthlyLimit(int perFamilyMonthlyLimit) {
        this.perFamilyMonthlyLimit = perFamilyMonthlyLimit;
    }

    public OffsetDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(OffsetDateTime validFrom) {
        this.validFrom = validFrom;
    }

    public OffsetDateTime getValidTo() {
        return validTo;
    }

    public void setValidTo(OffsetDateTime validTo) {
        this.validTo = validTo;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
