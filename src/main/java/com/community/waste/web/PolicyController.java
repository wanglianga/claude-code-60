package com.community.waste.web;

import com.community.waste.exception.ApiException;
import com.community.waste.model.Policy;
import com.community.waste.repo.BuildingRepo;
import com.community.waste.repo.BucketPointRepo;
import com.community.waste.service.PolicyService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/policies")
public class PolicyController {

    private final PolicyService policyService;
    private final BuildingRepo buildingRepo;
    private final BucketPointRepo bucketPointRepo;

    public PolicyController(PolicyService policyService, BuildingRepo buildingRepo,
                            BucketPointRepo bucketPointRepo) {
        this.policyService = policyService;
        this.buildingRepo = buildingRepo;
        this.bucketPointRepo = bucketPointRepo;
    }

    @GetMapping
    public List<Policy> list() {
        return policyService.list();
    }

    public record CreateRequest(@NotBlank String name, @NotNull String type, String description,
                                @NotNull LocalDate startDate, LocalDate endDate,
                                Long buildingId, Long bucketPointId) {
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('GOVERNANCE','ADMIN')")
    public Policy create(@RequestBody CreateRequest req) {
        Policy policy = new Policy();
        policy.setName(req.name());
        try {
            policy.setType(Policy.Type.valueOf(req.type()));
        } catch (Exception e) {
            throw ApiException.badRequest("政策类型不合法: " + req.type());
        }
        policy.setDescription(req.description());
        policy.setStartDate(req.startDate());
        policy.setEndDate(req.endDate());
        if (req.buildingId() != null) {
            policy.setBuilding(buildingRepo.findById(req.buildingId())
                    .orElseThrow(() -> ApiException.notFound("楼栋不存在")));
        }
        if (req.bucketPointId() != null) {
            policy.setBucketPoint(bucketPointRepo.findById(req.bucketPointId())
                    .orElseThrow(() -> ApiException.notFound("桶点不存在")));
        }
        return policyService.create(policy);
    }

    /** 政策前后对比：参与度与误投变化。 */
    @GetMapping("/{id}/compare")
    @PreAuthorize("hasAnyRole('GOVERNANCE','PROPERTY','ADMIN')")
    public Map<String, Object> compare(@PathVariable Long id, @RequestParam(defaultValue = "30") int days) {
        return policyService.compare(id, days);
    }
}
