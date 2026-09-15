package com.community.waste.service;

import com.community.waste.config.AppProperties;
import com.community.waste.exception.ApiException;
import com.community.waste.model.*;
import com.community.waste.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class RedemptionService {

    private final ProductRepo productRepo;
    private final RedemptionOrderRepo orderRepo;
    private final RedemptionReservationRepo reservationRepo;
    private final ViolationRepo violationRepo;
    private final PointsService pointsService;
    private final AppProperties props;

    public RedemptionService(ProductRepo productRepo, RedemptionOrderRepo orderRepo,
                             RedemptionReservationRepo reservationRepo,
                             ViolationRepo violationRepo, PointsService pointsService, AppProperties props) {
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
        this.reservationRepo = reservationRepo;
        this.violationRepo = violationRepo;
        this.pointsService = pointsService;
        this.props = props;
    }

    /** 历史违规拦截（兑换与预约共用）。 */
    public void checkViolations(AppUser user) {
        OffsetDateTime now = OffsetDateTime.now();
        long violations = violationRepo.countByUserIdAndLevelInAndCreatedAtAfter(
                user.getId(),
                List.of(Violation.Level.MINOR, Violation.Level.MAJOR),
                now.minusDays(props.getRules().getRedeemViolationDays()));
        if (violations >= props.getRules().getRedeemViolationLimit()) {
            throw ApiException.conflict(String.format("最近 %d 天内有 %d 次违规记录，暂不可兑换，请先完成教育学习",
                    props.getRules().getRedeemViolationDays(), violations));
        }
    }

    /** 商品可兑换性检查（兑换与预约共用）。 */
    public Product checkProductAvailable(Long productId) {
        Product product = productRepo.findById(productId).orElseThrow(() -> ApiException.notFound("商品不存在"));
        if (!product.isActive()) {
            throw ApiException.conflict("商品已下架");
        }
        OffsetDateTime now = OffsetDateTime.now();
        if (product.getValidFrom() != null && now.isBefore(product.getValidFrom())) {
            throw ApiException.conflict("商品尚未开始兑换");
        }
        if (product.isExpired()) {
            throw ApiException.conflict("商品已过期，不可兑换");
        }
        return product;
    }

    /** 月度限购检查（个人 + 家庭，订单与排队预约合并计数）。 */
    public void checkMonthlyLimits(AppUser user, Product product, int quantity) {
        OffsetDateTime monthStart = YearMonth.now().atDay(1).atStartOfDay(ZoneId.systemDefault()).toOffsetDateTime();
        List<RedemptionOrder.Status> orderStatuses = List.of(RedemptionOrder.Status.PENDING, RedemptionOrder.Status.FULFILLED);
        List<RedemptionReservation.Status> rsvStatuses = List.of(RedemptionReservation.Status.WAITING,
                RedemptionReservation.Status.READY, RedemptionReservation.Status.FULFILLED);
        long userUsed = orderRepo.sumQuantityByUserAndProductSince(user.getId(), product.getId(), orderStatuses, monthStart)
                + reservationRepo.sumQuantityByUserAndProductSince(user.getId(), product.getId(), rsvStatuses, monthStart);
        if (userUsed + quantity > product.getPerUserMonthlyLimit()) {
            throw ApiException.conflict(String.format("超出个人每月限购（%d 件），本月已兑/约 %d 件",
                    product.getPerUserMonthlyLimit(), userUsed));
        }
        if (user.getFamily() != null) {
            long familyUsed = orderRepo.sumQuantityByFamilyAndProductSince(
                    user.getFamily().getId(), product.getId(), orderStatuses, monthStart)
                    + reservationRepo.sumQuantityByFamilyAndProductSince(
                    user.getFamily().getId(), product.getId(), rsvStatuses, monthStart);
            if (familyUsed + quantity > product.getPerFamilyMonthlyLimit()) {
                throw ApiException.conflict(String.format("超出家庭每月限购（%d 件），本家庭已兑/约 %d 件",
                        product.getPerFamilyMonthlyLimit(), familyUsed));
            }
        }
    }

    /**
     * 发起兑换：检查库存、兑换限制（个人/家庭月度）、历史违规，积分不足时家庭成员共享代付。
     */
    @Transactional
    public RedemptionOrder redeem(AppUser user, Long productId, int quantity) {
        if (quantity <= 0) {
            throw ApiException.badRequest("数量必须大于 0");
        }
        Product product = checkProductAvailable(productId);
        if (product.getStock() < quantity) {
            throw ApiException.conflict("库存不足，当前库存 " + product.getStock()
                    + "，可通过 POST /api/queue 预约排队，到货后按顺序分配");
        }
        checkViolations(user);
        checkMonthlyLimits(user, product, quantity);

        int totalPoints = product.getCostPoints() * quantity;

        RedemptionOrder order = new RedemptionOrder();
        order.setOrderNo("R" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase());
        order.setUser(user);
        order.setFamily(user.getFamily());
        order.setProduct(product);
        order.setQuantity(quantity);
        order.setPointsSpent(totalPoints);
        order = orderRepo.save(order);

        // 扣积分：先扣本人，不足部分家庭成员共享代付；不足时抛异常整体回滚
        int paid = pointsService.deductWithFamilyShare(user, totalPoints,
                PointsTransaction.TxType.REDEMPTION, PointsTransaction.TxType.REDEMPTION_SHARE,
                "REDEMPTION_ORDER", order.getId(), "兑换 " + product.getName());
        if (paid < totalPoints) {
            throw ApiException.conflict("积分不足，家庭共享后仍差 " + (totalPoints - paid) + " 分");
        }

        product.setStock(product.getStock() - quantity);
        productRepo.save(product);
        return order;
    }

    /** 核销（物业/管理员）。 */
    @Transactional
    public RedemptionOrder fulfill(Long orderId, AppUser operator) {
        RedemptionOrder order = orderRepo.findById(orderId).orElseThrow(() -> ApiException.notFound("订单不存在"));
        if (order.getStatus() != RedemptionOrder.Status.PENDING) {
            throw ApiException.conflict("订单状态不可核销: " + order.getStatus());
        }
        order.setStatus(RedemptionOrder.Status.FULFILLED);
        order.setFulfilledAt(OffsetDateTime.now());
        return orderRepo.save(order);
    }

    /** 取消（本人，待核销状态），退回库存与积分。 */
    @Transactional
    public RedemptionOrder cancel(Long orderId, AppUser operator) {
        RedemptionOrder order = orderRepo.findById(orderId).orElseThrow(() -> ApiException.notFound("订单不存在"));
        boolean owner = order.getUser().getId().equals(operator.getId());
        boolean staff = operator.getRole() == AppUser.Role.ADMIN || operator.getRole() == AppUser.Role.PROPERTY;
        if (!owner && !staff) {
            throw ApiException.forbidden("只能取消自己的订单");
        }
        if (order.getStatus() != RedemptionOrder.Status.PENDING) {
            throw ApiException.conflict("仅待核销订单可取消");
        }
        order.setStatus(RedemptionOrder.Status.CANCELLED);
        Product product = order.getProduct();
        product.setStock(product.getStock() + order.getQuantity());
        productRepo.save(product);
        pointsService.refundByRef("REDEMPTION_ORDER", order.getId(),
                PointsTransaction.TxType.ADJUST, "订单取消退回积分");
        return orderRepo.save(order);
    }

    @Transactional(readOnly = true)
    public List<RedemptionOrder> listMine(AppUser user) {
        return orderRepo.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public List<RedemptionOrder> listPending() {
        return orderRepo.findByStatusOrderByCreatedAtAsc(RedemptionOrder.Status.PENDING);
    }
}
