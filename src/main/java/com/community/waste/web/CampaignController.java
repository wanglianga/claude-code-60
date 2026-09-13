package com.community.waste.web;

import com.community.waste.config.CurrentUser;
import com.community.waste.model.Campaign;
import com.community.waste.model.CampaignSignup;
import com.community.waste.service.CampaignService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService campaignService;
    private final CurrentUser currentUser;

    public CampaignController(CampaignService campaignService, CurrentUser currentUser) {
        this.campaignService = campaignService;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return campaignService.listWithStats();
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('GOVERNANCE','PROPERTY','ADMIN')")
    public Campaign create(@RequestBody Campaign campaign) {
        return campaignService.create(campaign);
    }

    /** 居民报名宣传活动。 */
    @PostMapping("/{id}/signup")
    public CampaignSignup signup(@PathVariable Long id) {
        return campaignService.signup(currentUser.require(), id);
    }

    @PostMapping("/signups/{signupId}/attend")
    @PreAuthorize("hasAnyRole('GOVERNANCE','SUPERVISOR','ADMIN')")
    public CampaignSignup attend(@PathVariable Long signupId) {
        return campaignService.attend(signupId, currentUser.require());
    }

    @GetMapping("/{id}/signups")
    @PreAuthorize("hasAnyRole('GOVERNANCE','PROPERTY','SUPERVISOR','ADMIN')")
    public List<CampaignSignup> signups(@PathVariable Long id) {
        return campaignService.signupsOf(id);
    }
}
