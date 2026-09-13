package com.community.waste.web;

import com.community.waste.config.CurrentUser;
import com.community.waste.model.AppUser;
import com.community.waste.model.DisposalRecord;
import com.community.waste.service.DisposalService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disposals")
public class DisposalController {

    private final DisposalService disposalService;
    private final CurrentUser currentUser;

    public DisposalController(DisposalService disposalService, CurrentUser currentUser) {
        this.disposalService = disposalService;
        this.currentUser = currentUser;
    }

    /** 登记投放（居民扫码 / 督导员代录 / 摄像头识别带 detectedIssues）。 */
    @PostMapping
    public DisposalRecord create(@RequestBody DisposalService.CreateRequest req) {
        return disposalService.create(currentUser.require(), req);
    }

    @GetMapping("/mine")
    public List<DisposalRecord> mine() {
        return disposalService.listMine(currentUser.require());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SUPERVISOR','PROPERTY','GOVERNANCE','ADMIN')")
    public List<DisposalRecord> all() {
        return disposalService.listAll();
    }
}
