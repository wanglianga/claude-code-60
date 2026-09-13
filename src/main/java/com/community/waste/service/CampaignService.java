package com.community.waste.service;

import com.community.waste.exception.ApiException;
import com.community.waste.model.*;
import com.community.waste.repo.CampaignRepo;
import com.community.waste.repo.CampaignSignupRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CampaignService {

    private final CampaignRepo campaignRepo;
    private final CampaignSignupRepo signupRepo;
    private final PointsService pointsService;

    public CampaignService(CampaignRepo campaignRepo, CampaignSignupRepo signupRepo, PointsService pointsService) {
        this.campaignRepo = campaignRepo;
        this.signupRepo = signupRepo;
        this.pointsService = pointsService;
    }

    @Transactional
    public Campaign create(Campaign campaign) {
        if (campaign.getEndAt().isBefore(campaign.getStartAt())) {
            throw ApiException.badRequest("结束时间不能早于开始时间");
        }
        return campaignRepo.save(campaign);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listWithStats() {
        return campaignRepo.findAll().stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", c.getId());
            m.put("title", c.getTitle());
            m.put("description", c.getDescription());
            m.put("startAt", c.getStartAt());
            m.put("endAt", c.getEndAt());
            m.put("capacity", c.getCapacity());
            m.put("pointsReward", c.getPointsReward());
            m.put("signupCount", signupRepo.countByCampaignId(c.getId()));
            m.put("attendedCount", signupRepo.countByCampaignIdAndAttendedTrue(c.getId()));
            return m;
        }).toList();
    }

    /** 居民报名宣传活动。 */
    @Transactional
    public CampaignSignup signup(AppUser user, Long campaignId) {
        Campaign campaign = campaignRepo.findById(campaignId).orElseThrow(() -> ApiException.notFound("活动不存在"));
        if (OffsetDateTime.now().isAfter(campaign.getEndAt())) {
            throw ApiException.conflict("活动已结束，不可报名");
        }
        if (signupRepo.existsByCampaignIdAndUserId(campaignId, user.getId())) {
            throw ApiException.conflict("已报名该活动");
        }
        if (signupRepo.countByCampaignId(campaignId) >= campaign.getCapacity()) {
            throw ApiException.conflict("活动名额已满");
        }
        CampaignSignup signup = new CampaignSignup();
        signup.setCampaign(campaign);
        signup.setUser(user);
        return signupRepo.save(signup);
    }

    /** 签到核销（治理人员/督导员），到场即发奖励积分。 */
    @Transactional
    public CampaignSignup attend(Long signupId, AppUser operator) {
        CampaignSignup signup = signupRepo.findById(signupId).orElseThrow(() -> ApiException.notFound("报名记录不存在"));
        if (signup.isAttended()) {
            throw ApiException.conflict("已签到");
        }
        signup.setAttended(true);
        signup.setAttendedAt(OffsetDateTime.now());
        Campaign campaign = signup.getCampaign();
        if (campaign.getPointsReward() > 0) {
            pointsService.apply(signup.getUser(), campaign.getPointsReward(),
                    PointsTransaction.TxType.CAMPAIGN_REWARD, "CAMPAIGN", campaign.getId(),
                    "参加宣传活动奖励：" + campaign.getTitle());
        }
        return signupRepo.save(signup);
    }

    @Transactional(readOnly = true)
    public List<CampaignSignup> signupsOf(Long campaignId) {
        return signupRepo.findByCampaignId(campaignId);
    }
}
