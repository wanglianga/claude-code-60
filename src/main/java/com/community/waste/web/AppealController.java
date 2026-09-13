package com.community.waste.web;

import com.community.waste.config.CurrentUser;
import com.community.waste.model.Appeal;
import com.community.waste.service.AppealService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/appeals")
public class AppealController {

    private final AppealService appealService;
    private final CurrentUser currentUser;

    public AppealController(AppealService appealService, CurrentUser currentUser) {
        this.appealService = appealService;
        this.currentUser = currentUser;
    }

    public record FileRequest(@NotNull Long pointsTransactionId, @NotBlank String reason) {
    }

    /** 居民申诉扣分。 */
    @PostMapping
    public Appeal file(@RequestBody FileRequest req) {
        return appealService.file(currentUser.require(), req.pointsTransactionId(), req.reason());
    }

    @GetMapping("/mine")
    public List<Appeal> mine() {
        return appealService.listMine(currentUser.require());
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('GOVERNANCE','ADMIN')")
    public List<Appeal> pending() {
        return appealService.listPending();
    }

    public record HandleRequest(boolean approve, String note) {
    }

    /** 社区治理人员处理申诉。 */
    @PostMapping("/{id}/handle")
    @PreAuthorize("hasAnyRole('GOVERNANCE','ADMIN')")
    public Appeal handle(@PathVariable Long id, @RequestBody HandleRequest req) {
        return appealService.handle(id, currentUser.require(), req.approve(), req.note());
    }
}
