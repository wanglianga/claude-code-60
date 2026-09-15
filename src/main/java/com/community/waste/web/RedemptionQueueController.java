package com.community.waste.web;

import com.community.waste.config.CurrentUser;
import com.community.waste.model.RedemptionReservation;
import com.community.waste.model.RestockPlan;
import com.community.waste.service.RedemptionQueueService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@RestController
public class RedemptionQueueController {

    private final RedemptionQueueService queueService;
    private final CurrentUser currentUser;

    public RedemptionQueueController(RedemptionQueueService queueService, CurrentUser currentUser) {
        this.queueService = queueService;
        this.currentUser = currentUser;
    }

    public record JoinRequest(@NotNull Long productId, @Min(1) int quantity) {
    }

    /** 库存不足时预约排队：冻结积分，返回位次/预计到货/替代商品。 */
    @PostMapping("/api/queue")
    public Map<String, Object> join(@RequestBody JoinRequest req) {
        return queueService.join(currentUser.require(), req.productId(), req.quantity());
    }

    @GetMapping("/api/queue/mine")
    public List<Map<String, Object>> mine() {
        return queueService.mine(currentUser.require());
    }

    @GetMapping("/api/queue")
    @PreAuthorize("hasAnyRole('PROPERTY','GOVERNANCE','ADMIN')")
    public List<Map<String, Object>> queue(@RequestParam(required = false) Long productId) {
        return queueService.queueOf(productId);
    }

    @PostMapping("/api/queue/{id}/cancel")
    public RedemptionReservation cancel(@PathVariable Long id) {
        return queueService.cancel(id, currentUser.require());
    }

    /** 实际领取（到货后）。 */
    @PostMapping("/api/queue/{id}/pickup")
    public RedemptionReservation pickup(@PathVariable Long id) {
        return queueService.pickup(id, currentUser.require());
    }

    public record AltPickupRequest(@NotNull Long altProductId) {
    }

    /** 领取替代商品：消耗积分，原预约保留排队顺序。 */
    @PostMapping("/api/queue/{id}/alt-pickup")
    public Map<String, Object> altPickup(@PathVariable Long id, @RequestBody AltPickupRequest req) {
        return queueService.altPickup(id, req.altProductId(), currentUser.require());
    }

    /** 手动触发到期回滚（正常由定时任务自动执行）。 */
    @PostMapping("/api/queue/process-expiries")
    @PreAuthorize("hasAnyRole('PROPERTY','GOVERNANCE','ADMIN')")
    public Map<String, Object> processExpiries() {
        int n = queueService.processExpiries();
        return Map.of("expired", n);
    }

    /** 采购建议：高需求商品缺口，纳入下一次采购计划。 */
    @GetMapping("/api/queue/suggestions")
    @PreAuthorize("hasAnyRole('PROPERTY','GOVERNANCE','ADMIN')")
    public List<Map<String, Object>> suggestions() {
        return queueService.suggestions();
    }

    public record PlanRequest(@NotNull Long productId, @Min(1) int quantity,
                              @NotNull OffsetDateTime expectedAt, String note) {
    }

    @PostMapping("/api/restock-plans")
    @PreAuthorize("hasAnyRole('PROPERTY','GOVERNANCE','ADMIN')")
    public RestockPlan createPlan(@RequestBody PlanRequest req) {
        return queueService.createPlan(currentUser.require(), req.productId(), req.quantity(),
                req.expectedAt(), req.note());
    }

    @GetMapping("/api/restock-plans")
    @PreAuthorize("hasAnyRole('PROPERTY','GOVERNANCE','COLLECTOR','ADMIN')")
    public List<RestockPlan> listPlans(@RequestParam(required = false) Long productId) {
        return queueService.listPlans(productId);
    }

    /** 供应商到货，入库并自动按排队顺序分配。 */
    @PostMapping("/api/restock-plans/{id}/arrive")
    @PreAuthorize("hasAnyRole('PROPERTY','COLLECTOR','ADMIN')")
    public RestockPlan arrive(@PathVariable Long id) {
        return queueService.arrive(id, currentUser.require());
    }
}
