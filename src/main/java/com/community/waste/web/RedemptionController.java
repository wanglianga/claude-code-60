package com.community.waste.web;

import com.community.waste.config.CurrentUser;
import com.community.waste.model.RedemptionOrder;
import com.community.waste.service.RedemptionService;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/redemptions")
public class RedemptionController {

    private final RedemptionService redemptionService;
    private final CurrentUser currentUser;

    public RedemptionController(RedemptionService redemptionService, CurrentUser currentUser) {
        this.redemptionService = redemptionService;
        this.currentUser = currentUser;
    }

    public record RedeemRequest(@NotNull Long productId, @Min(1) int quantity) {
    }

    /** 居民兑换：检查库存、个人/家庭限购、历史违规，支持家庭积分共享。 */
    @PostMapping
    public RedemptionOrder redeem(@RequestBody RedeemRequest req) {
        return redemptionService.redeem(currentUser.require(), req.productId(), req.quantity());
    }

    @GetMapping("/mine")
    public List<RedemptionOrder> mine() {
        return redemptionService.listMine(currentUser.require());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('PROPERTY','ADMIN')")
    public List<RedemptionOrder> pending() {
        return redemptionService.listPending();
    }

    @PostMapping("/{id}/fulfill")
    @PreAuthorize("hasAnyRole('PROPERTY','ADMIN')")
    public RedemptionOrder fulfill(@PathVariable Long id) {
        return redemptionService.fulfill(id, currentUser.require());
    }

    @PostMapping("/{id}/cancel")
    public RedemptionOrder cancel(@PathVariable Long id) {
        return redemptionService.cancel(id, currentUser.require());
    }
}
