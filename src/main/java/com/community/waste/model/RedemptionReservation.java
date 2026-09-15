package com.community.waste.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

import java.time.OffsetDateTime;

/**
 * 兑换预约排队：库存不足时居民预约，到货后按顺序分配，到期未领自动回滚。
 */
@Entity
@Table(name = "redemption_reservations")
public class RedemptionReservation {

    public enum Status {
        WAITING,    // 排队中
        READY,      // 已分配到货，待领取
        FULFILLED,  // 已领取
        CANCELLED,  // 已取消（退积分）
        EXPIRED     // 到期未领取，自动回滚（退积分）
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
    @JoinColumn(name = "family_id")
    private Family family;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "points_reserved", nullable = false)
    private int pointsReserved;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Status status = Status.WAITING;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "ready_at")
    private OffsetDateTime readyAt;

    @Column(name = "expire_at")
    private OffsetDateTime expireAt;

    @Column(name = "fulfilled_at")
    private OffsetDateTime fulfilledAt;

    @Column(name = "cancelled_at")
    private OffsetDateTime cancelledAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alt_product_id")
    private Product altProduct;

    @Column(name = "alt_order_id")
    private Long altOrderId;

    @Column(name = "alt_fulfilled_at")
    private OffsetDateTime altFulfilledAt;

    @JsonProperty("userId")
    public Long userId() {
        return user == null ? null : user.getId();
    }

    @JsonProperty("userName")
    public String userName() {
        return user == null ? null : user.getDisplayName();
    }

    @JsonProperty("productId")
    public Long productId() {
        return product == null ? null : product.getId();
    }

    @JsonProperty("productName")
    public String productName() {
        return product == null ? null : product.getName();
    }

    @JsonProperty("altProductName")
    public String altProductName() {
        return altProduct == null ? null : altProduct.getName();
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

    public Family getFamily() {
        return family;
    }

    public void setFamily(Family family) {
        this.family = family;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public int getPointsReserved() {
        return pointsReserved;
    }

    public void setPointsReserved(int pointsReserved) {
        this.pointsReserved = pointsReserved;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getReadyAt() {
        return readyAt;
    }

    public void setReadyAt(OffsetDateTime readyAt) {
        this.readyAt = readyAt;
    }

    public OffsetDateTime getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(OffsetDateTime expireAt) {
        this.expireAt = expireAt;
    }

    public OffsetDateTime getFulfilledAt() {
        return fulfilledAt;
    }

    public void setFulfilledAt(OffsetDateTime fulfilledAt) {
        this.fulfilledAt = fulfilledAt;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(OffsetDateTime cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Product getAltProduct() {
        return altProduct;
    }

    public void setAltProduct(Product altProduct) {
        this.altProduct = altProduct;
    }

    public Long getAltOrderId() {
        return altOrderId;
    }

    public void setAltOrderId(Long altOrderId) {
        this.altOrderId = altOrderId;
    }

    public OffsetDateTime getAltFulfilledAt() {
        return altFulfilledAt;
    }

    public void setAltFulfilledAt(OffsetDateTime altFulfilledAt) {
        this.altFulfilledAt = altFulfilledAt;
    }
}
