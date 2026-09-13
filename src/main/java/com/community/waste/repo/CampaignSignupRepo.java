package com.community.waste.repo;

import com.community.waste.model.CampaignSignup;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CampaignSignupRepo extends JpaRepository<CampaignSignup, Long> {

    long countByCampaignId(Long campaignId);

    long countByCampaignIdAndAttendedTrue(Long campaignId);

    boolean existsByCampaignIdAndUserId(Long campaignId, Long userId);

    List<CampaignSignup> findByCampaignId(Long campaignId);

    List<CampaignSignup> findByUserId(Long userId);
}
