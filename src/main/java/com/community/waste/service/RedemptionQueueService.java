package com.community.waste.service;

import com.community.waste.config.AppProperties;
import com.community.waste.exception.ApiException;
import com.community.waste.model.*;
import com.community.waste.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

/**
 * 兑换库存联动：缺货预约排队、供应商到货 FIFO 分配、到期未领自动回滚、替代商品领取、采购建议。
 */
@Service
public class RedemptionQueueService {

    private static final Logger log = LoggerFactory.getLogger(RedemptionQueueService.class);

    private final RedemptionReservationRepo reservationRepo;
    private final RestockPlanRepo restockPlanRepo;
    private final ProductRepo productRepo;
    private final RedemptionOrderRepo orderRepo;
    private final RedemptionService redemptionService;
    private final PointsService pointsService;
    private final AppProperties props;

    public RedemptionQueueService(RedemptionReservationRepo reservationRepo, RestockPlanRepo restockPlanRepo,
                                  ProductRepo productRepo, RedemptionOrderRepo orderRepo,
                                  RedemptionService redemptionService, PointsService pointsService,
                                  AppProperties props) {
        this.reservationRepo = reservationRepo;
        this.restockPlanRepo = restockPlanRepo;
        this.productRepo = productRepo;
        this.orderRepo = orderRepo;
        this.redemptionService = redemptionService;
        this.pointsService = pointsService;
        this.props = props;
    }

    /**
     * 预约排队：库存不足时冻结积分（支持家庭共享），按预约时间先后排队。
     * 返回位次、预计到货时间与替代商品建议。
     */
    @Transactional
    public Map<String, Object> join(AppUser user, Long productId, int quantity) {
        if (quantity <= 0) {
            throw ApiException.badRequest("数量必须大于 0");
        }
        Product product = redemptionService.checkProductAvailable(productId);
        if (product.getStock() >= quantity) {
            throw ApiException.conflict("当前库存充足，请直接兑换，无需排队");
        }
        redemptionService.checkViolations(user);
        redemptionService.checkMonthlyLimits(user, product, quantity);

        RedemptionReservation reservation = new RedemptionReservation();
        reservation.setUser(user);
        reservation.setFamily(user.getFamily());
        reservation.setProduct(product);
        reservation.setQuantity(quantity);
        int totalPoints = product.getCostPoints() * quantity;
        reservation.setPointsReserved(totalPoints);
        reservation = reservationRepo.save(reservation);

        // 冻结积分（本人 + 家庭共享）；不足则整体回滚
        int paid = pointsService.deductWithFamilyShare(user, totalPoints,
                PointsTransaction.TxType.RESERVATION, PointsTransaction.TxType.RESERVATION_SHARE,
                "RESERVATION", reservation.getId(), "预约排队 " + product.getName());
        if (paid < totalPoints) {
            throw ApiException.conflict("积分不足，家庭共享后仍差 " + (totalPoints - paid) + " 分");
        }
        log.info("预约排队: {} 预约 {} x{}，冻结 {} 分", user.getDisplayName(), product.getName(), quantity, totalPoints);
        return enrich(reservation);
    }

    /** 我的预约列表（含位次/预计到货/替代商品）。 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> mine(AppUser user) {
        return reservationRepo.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::enrich)
                .toList();
    }

    /** 工作人员查看排队（可按商品过滤）。 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> queueOf(Long productId) {
        List<RedemptionReservation> waiting = productId == null
                ? reservationRepo.findByStatusOrderByCreatedAtAsc(RedemptionReservation.Status.WAITING)
                : reservationRepo.findByProductIdAndStatusOrderByCreatedAtAsc(productId, RedemptionReservation.Status.WAITING);
        return waiting.stream().map(this::enrich).toList();
    }

    /**
     * 取消预约：WAITING 直接退；READY 取消需回补库存并重新分配给后续排队者。积分原路退回。
     */
    @Transactional
    public RedemptionReservation cancel(Long id, AppUser operator) {
        RedemptionReservation r = reservationRepo.findById(id).orElseThrow(() -> ApiException.notFound("预约不存在"));
        boolean owner = r.getUser().getId().equals(operator.getId());
        boolean staff = operator.getRole() == AppUser.Role.ADMIN || operator.getRole() == AppUser.Role.PROPERTY;
        if (!owner && !staff) {
            throw ApiException.forbidden("只能取消自己的预约");
        }
        if (r.getStatus() != RedemptionReservation.Status.WAITING
                && r.getStatus() != RedemptionReservation.Status.READY) {
            throw ApiException.conflict("当前状态不可取消: " + r.getStatus());
        }
        boolean wasReady = r.getStatus() == RedemptionReservation.Status.READY;
        r.setStatus(RedemptionReservation.Status.CANCELLED);
        r.setCancelledAt(OffsetDateTime.now());
        pointsService.refundByRef("RESERVATION", r.getId(),
                PointsTransaction.TxType.RESERVATION_REFUND, "预约取消，退回冻结积分");
        RedemptionReservation saved = reservationRepo.save(r);
        if (wasReady) {
            Product product = r.getProduct();
            product.setStock(product.getStock() + r.getQuantity());
            productRepo.save(product);
            allocate(product);
        }
        return saved;
    }

    /** 实际领取（READY → FULFILLED），本人或物业核销。 */
    @Transactional
    public RedemptionReservation pickup(Long id, AppUser operator) {
        RedemptionReservation r = reservationRepo.findById(id).orElseThrow(() -> ApiException.notFound("预约不存在"));
        boolean owner = r.getUser().getId().equals(operator.getId());
        boolean staff = operator.getRole() == AppUser.Role.ADMIN || operator.getRole() == AppUser.Role.PROPERTY;
        if (!owner && !staff) {
            throw ApiException.forbidden("只能领取自己的预约");
        }
        if (r.getStatus() != RedemptionReservation.Status.READY) {
            throw ApiException.conflict("仅到货待领取状态可领取，当前: " + r.getStatus());
        }
        if (r.getExpireAt() != null && OffsetDateTime.now().isAfter(r.getExpireAt())) {
            throw ApiException.conflict("已过领取截止时间，预约将自动回滚");
        }
        r.setStatus(RedemptionReservation.Status.FULFILLED);
        r.setFulfilledAt(OffsetDateTime.now());
        return reservationRepo.save(r);
    }

    /**
     * 领取替代商品：消耗积分（扣替代品积分、减库存、生成已核销订单），原预约保留排队顺序。
     */
    @Transactional
    public Map<String, Object> altPickup(Long reservationId, Long altProductId, AppUser user) {
        RedemptionReservation r = reservationRepo.findById(reservationId)
                .orElseThrow(() -> ApiException.notFound("预约不存在"));
        if (!r.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("只能为自己的预约领取替代商品");
        }
        if (r.getStatus() != RedemptionReservation.Status.WAITING) {
            throw ApiException.conflict("仅排队中的预约可领取替代商品，当前: " + r.getStatus());
        }
        if (r.getAltFulfilledAt() != null) {
            throw ApiException.conflict("该预约已领取过替代商品");
        }
        if (altProductId.equals(r.getProduct().getId())) {
            throw ApiException.badRequest("替代商品不能是原商品本身");
        }
        int quantity = r.getQuantity();
        Product alt = redemptionService.checkProductAvailable(altProductId);
        if (alt.getStock() < quantity) {
            throw ApiException.conflict("替代商品库存不足，当前库存 " + alt.getStock());
        }
        redemptionService.checkViolations(user);
        redemptionService.checkMonthlyLimits(user, alt, quantity);

        // 替代商品直接核销领取
        int totalPoints = alt.getCostPoints() * quantity;
        RedemptionOrder order = new RedemptionOrder();
        order.setOrderNo("RA" + UUID.randomUUID().toString().replace("-", "").substring(0, 14).toUpperCase());
        order.setUser(user);
        order.setFamily(user.getFamily());
        order.setProduct(alt);
        order.setQuantity(quantity);
        order.setPointsSpent(totalPoints);
        order.setStatus(RedemptionOrder.Status.FULFILLED);
        order.setFulfilledAt(OffsetDateTime.now());
        order = orderRepo.save(order);

        int paid = pointsService.deductWithFamilyShare(user, totalPoints,
                PointsTransaction.TxType.REDEMPTION, PointsTransaction.TxType.REDEMPTION_SHARE,
                "REDEMPTION_ORDER", order.getId(), "替代商品领取 " + alt.getName() + "（原预约 " + r.getProduct().getName() + " 保留排队）");
        if (paid < totalPoints) {
            throw ApiException.conflict("积分不足，家庭共享后仍差 " + (totalPoints - paid) + " 分");
        }
        alt.setStock(alt.getStock() - quantity);
        productRepo.save(alt);

        // 原预约保留排队顺序，仅记录替代领取信息
        r.setAltProduct(alt);
        r.setAltOrderId(order.getId());
        r.setAltFulfilledAt(OffsetDateTime.now());
        reservationRepo.save(r);

        Map<String, Object> result = new LinkedHashMap<>(enrich(r));
        result.put("altOrder", order);
        return result;
    }

    /** 供应商到货：入库并按排队顺序 FIFO 分配。 */
    @Transactional
    public RestockPlan arrive(Long planId, AppUser operator) {
        RestockPlan plan = restockPlanRepo.findById(planId).orElseThrow(() -> ApiException.notFound("补货计划不存在"));
        if (plan.getStatus() != RestockPlan.Status.PLANNED) {
            throw ApiException.conflict("该计划状态不可到货: " + plan.getStatus());
        }
        plan.setStatus(RestockPlan.Status.ARRIVED);
        plan.setArrivedAt(OffsetDateTime.now());
        Product product = plan.getProduct();
        product.setStock(product.getStock() + plan.getQuantity());
        productRepo.save(product);
        allocate(product);
        return restockPlanRepo.save(plan);
    }

    /** 创建补货/采购计划（高需求商品纳入下一次采购）。 */
    @Transactional
    public RestockPlan createPlan(AppUser operator, Long productId, int quantity, OffsetDateTime expectedAt, String note) {
        if (quantity <= 0) {
            throw ApiException.badRequest("数量必须大于 0");
        }
        Product product = productRepo.findById(productId).orElseThrow(() -> ApiException.notFound("商品不存在"));
        RestockPlan plan = new RestockPlan();
        plan.setProduct(product);
        plan.setQuantity(quantity);
        plan.setExpectedAt(expectedAt);
        plan.setNote(note);
        plan.setCreatedBy(operator);
        return restockPlanRepo.save(plan);
    }

    @Transactional(readOnly = true)
    public List<RestockPlan> listPlans(Long productId) {
        return productId == null
                ? restockPlanRepo.findAll()
                : restockPlanRepo.findByProductIdOrderByCreatedAtDesc(productId);
    }

    /**
     * FIFO 分配：库存按预约时间顺序分配给排队者，分配到即进入 READY（限时领取）。
     */
    private void allocate(Product product) {
        List<RedemptionReservation> waiting = reservationRepo
                .findByProductIdAndStatusOrderByCreatedAtAsc(product.getId(), RedemptionReservation.Status.WAITING);
        if (waiting.isEmpty()) {
            return;
        }
        int stock = product.getStock();
        OffsetDateTime now = OffsetDateTime.now();
        for (RedemptionReservation r : waiting) {
            if (r.getQuantity() > stock) {
                break; // 严格 FIFO：队首无法满足则不跳过
            }
            stock -= r.getQuantity();
            r.setStatus(RedemptionReservation.Status.READY);
            r.setReadyAt(now);
            r.setExpireAt(now.plusMinutes(props.getRules().getReservationPickupMinutes()));
            reservationRepo.save(r);
            log.info("预约分配到货: 预约#{} -> {}，请于 {} 前领取", r.getId(), r.getUser().getDisplayName(), r.getExpireAt());
        }
        product.setStock(stock);
        productRepo.save(product);
    }

    /**
     * 到期回滚：READY 超过领取期限的预约自动关闭、退回冻结积分、库存回补并重新分配。
     * 定时执行，也可由工作人员手动触发，避免手工登记。
     */
    @Transactional
    public int processExpiries() {
        OffsetDateTime now = OffsetDateTime.now();
        List<RedemptionReservation> expired = reservationRepo
                .findByStatusAndExpireAtBefore(RedemptionReservation.Status.READY, now);
        Set<Long> affectedProducts = new HashSet<>();
        for (RedemptionReservation r : expired) {
            r.setStatus(RedemptionReservation.Status.EXPIRED);
            r.setCancelledAt(now);
            reservationRepo.save(r);
            Product product = r.getProduct();
            product.setStock(product.getStock() + r.getQuantity());
            productRepo.save(product);
            pointsService.refundByRef("RESERVATION", r.getId(),
                    PointsTransaction.TxType.RESERVATION_REFUND, "预约到期未领取，自动退回积分");
            affectedProducts.add(product.getId());
            log.info("预约到期回滚: 预约#{}（{} x{}）已退积分并释放库存", r.getId(),
                    r.getUser().getDisplayName(), r.getQuantity());
        }
        for (Long productId : affectedProducts) {
            productRepo.findById(productId).ifPresent(this::allocate);
        }
        return expired.size();
    }

    @Scheduled(fixedDelayString = "${app.rules.watchdog-delay-ms:60000}", initialDelay = 45000)
    @Transactional
    public void expiryWatchdog() {
        processExpiries();
    }

    /** 采购建议：按排队需求量与在途补货计算缺口，供社区纳入下一次采购计划。 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> suggestions() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object[] row : reservationRepo.waitingStatsByProduct()) {
            Long productId = (Long) row[0];
            long waitingQty = ((Number) row[1]).longValue();
            long waitingCount = ((Number) row[2]).longValue();
            Product product = productRepo.findById(productId).orElse(null);
            if (product == null) {
                continue;
            }
            long planned = restockPlanRepo.sumPlannedQuantity(productId);
            long gap = waitingQty - product.getStock() - planned;
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("productId", productId);
            m.put("productName", product.getName());
            m.put("waitingReservations", waitingCount);
            m.put("waitingQuantity", waitingQty);
            m.put("currentStock", product.getStock());
            m.put("plannedRestock", planned);
            m.put("suggestedPurchase", Math.max(0, gap));
            m.put("nextExpectedAt", restockPlanRepo.findNextPlanned(productId)
                    .map(RestockPlan::getExpectedAt).orElse(null));
            result.add(m);
        }
        return result;
    }

    /** 替代商品建议：同类、有货、未过期，按积分差价排序取前 3。 */
    private List<Map<String, Object>> alternatives(Product product, int quantity) {
        return productRepo.findByActiveTrueOrderByCostPointsAsc().stream()
                .filter(p -> !p.getId().equals(product.getId()))
                .filter(p -> p.getCategory() == product.getCategory())
                .filter(p -> !p.isExpired())
                .filter(p -> p.getStock() >= quantity)
                .sorted(Comparator.comparingInt(p -> Math.abs(p.getCostPoints() - product.getCostPoints())))
                .limit(3)
                .map(p -> {
                    Map<String, Object> m = new LinkedHashMap<String, Object>();
                    m.put("id", p.getId());
                    m.put("name", p.getName());
                    m.put("costPoints", p.getCostPoints());
                    m.put("stock", p.getStock());
                    return m;
                })
                .toList();
    }

    /** 预约详情增强：位次、预计到货时间、替代商品。 */
    public Map<String, Object> enrich(RedemptionReservation r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("reservation", r);
        if (r.getStatus() == RedemptionReservation.Status.WAITING) {
            Long productId = r.getProduct().getId();
            m.put("position", reservationRepo.countWaitingAhead(productId, r.getCreatedAt()) + 1);
            m.put("estimatedArrival", restockPlanRepo.findNextPlanned(productId)
                    .map(RestockPlan::getExpectedAt).orElse(null));
            m.put("alternatives", alternatives(r.getProduct(), r.getQuantity()));
        }
        return m;
    }
}
