package com.community.waste.web;

import com.community.waste.config.CurrentUser;
import com.community.waste.model.AppUser;
import com.community.waste.model.ReviewTask;
import com.community.waste.service.ReviewService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/review-tasks")
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUser currentUser;

    public ReviewController(ReviewService reviewService, CurrentUser currentUser) {
        this.reviewService = reviewService;
        this.currentUser = currentUser;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR','PROPERTY','GOVERNANCE','ADMIN')")
    public List<ReviewTask> pending(@RequestParam(defaultValue = "false") boolean onlyMine) {
        return reviewService.listPending(currentUser.require(), onlyMine);
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('SUPERVISOR','PROPERTY','GOVERNANCE','ADMIN')")
    public List<ReviewTask> all() {
        return reviewService.listAll();
    }

    public record CompleteRequest(boolean confirmed, String action, String note) {
    }

    /** 督导员复核：确认误投（扣分/教育提醒）或判定误报。 */
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('SUPERVISOR','ADMIN')")
    public ReviewTask complete(@PathVariable Long id, @RequestBody CompleteRequest req) {
        return reviewService.complete(id, currentUser.require(), req.confirmed(), req.action(), req.note());
    }
}
