package com.community.waste.repo;

import com.community.waste.model.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampaignRepo extends JpaRepository<Campaign, Long> {
}
