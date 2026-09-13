package com.community.waste.service;

import com.community.waste.exception.ApiException;
import com.community.waste.model.Policy;
import com.community.waste.repo.DisposalRecordRepo;
import com.community.waste.repo.PolicyRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 政策效果评估：撤桶并点、定时投放、积分活动前后的参与度与误投变化。
 */
@Service
public class PolicyService {

    private final PolicyRepo policyRepo;
    private final DisposalRecordRepo disposalRepo;

    public PolicyService(PolicyRepo policyRepo, DisposalRecordRepo disposalRepo) {
        this.policyRepo = policyRepo;
        this.disposalRepo = disposalRepo;
    }

    @Transactional
    public Policy create(Policy policy) {
        return policyRepo.save(policy);
    }

    @Transactional(readOnly = true)
    public List<Policy> list() {
        return policyRepo.findAll();
    }

    /**
     * 对比政策前后各 days 天的居民参与度与误投率。
     * 范围：优先按楼栋，其次按桶点，都没有则全小区。
     */
    @Transactional(readOnly = true)
    public Map<String, Object> compare(Long policyId, int days) {
        Policy policy = policyRepo.findById(policyId).orElseThrow(() -> ApiException.notFound("政策不存在"));
        ZoneId zone = ZoneId.systemDefault();
        LocalDate start = policy.getStartDate();
        OffsetDateTime afterFrom = start.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime afterTo = start.plusDays(days).atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime beforeFrom = start.minusDays(days).atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime beforeTo = afterFrom;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("policy", policy);
        result.put("windowDays", days);
        result.put("before", window(beforeFrom, beforeTo, policy));
        result.put("after", window(afterFrom, afterTo, policy));
        return result;
    }

    private Map<String, Object> window(OffsetDateTime from, OffsetDateTime to, Policy policy) {
        long disposals;
        long missorts;
        long activeUsers;
        if (policy.getBuilding() != null) {
            Long bid = policy.getBuilding().getId();
            disposals = disposalRepo.countByBuildingAndRange(bid, from, to);
            missorts = disposalRepo.countMissortByBuildingAndRange(bid, from, to);
            activeUsers = disposalRepo.countDistinctUsersByBuildingAndRange(bid, from, to);
        } else if (policy.getBucketPoint() != null) {
            Long pid = policy.getBucketPoint().getId();
            disposals = disposalRepo.countByPointAndRange(pid, from, to);
            missorts = disposalRepo.countMissortByPointAndRange(pid, from, to);
            activeUsers = disposalRepo.countDistinctUsersByPointAndRange(pid, from, to);
        } else {
            disposals = disposalRepo.countByDisposedAtBetween(from, to);
            missorts = disposalRepo.countMissortByRange(from, to);
            activeUsers = disposalRepo.countDistinctUsersByRange(from, to);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("from", from.toLocalDate().toString());
        m.put("to", to.toLocalDate().toString());
        m.put("disposals", disposals);
        m.put("activeResidents", activeUsers);
        m.put("missorts", missorts);
        m.put("missortRate", disposals == 0 ? 0 : Math.round((double) missorts / disposals * 1000.0) / 1000.0);
        return m;
    }
}
