package com.community.waste.web;

import com.community.waste.config.CurrentUser;
import com.community.waste.exception.ApiException;
import com.community.waste.model.BucketEvent;
import com.community.waste.model.BucketPoint;
import com.community.waste.repo.BuildingRepo;
import com.community.waste.service.BucketPointService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/bucket-points")
public class BucketPointController {

    private final BucketPointService bucketPointService;
    private final BuildingRepo buildingRepo;
    private final CurrentUser currentUser;

    public BucketPointController(BucketPointService bucketPointService, BuildingRepo buildingRepo,
                                 CurrentUser currentUser) {
        this.bucketPointService = bucketPointService;
        this.buildingRepo = buildingRepo;
        this.currentUser = currentUser;
    }

    @GetMapping
    public List<BucketPoint> list() {
        return bucketPointService.list();
    }

    public record CreateRequest(@NotBlank String code, @NotBlank String name, @NotNull Long buildingId,
                                String address, String openStart, String openEnd, BigDecimal capacityKg) {
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('PROPERTY','ADMIN')")
    public BucketPoint create(@RequestBody CreateRequest req) {
        BucketPoint point = new BucketPoint();
        point.setCode(req.code());
        point.setName(req.name());
        point.setBuilding(buildingRepo.findById(req.buildingId())
                .orElseThrow(() -> ApiException.notFound("楼栋不存在")));
        point.setAddress(req.address());
        if (req.openStart() != null) {
            point.setOpenStart(req.openStart());
        }
        if (req.openEnd() != null) {
            point.setOpenEnd(req.openEnd());
        }
        if (req.capacityKg() != null) {
            point.setCapacityKg(req.capacityKg());
        }
        return bucketPointService.create(point);
    }

    public record MergeRequest(@NotNull Long intoId) {
    }

    /** 撤桶并点。 */
    @PostMapping("/{id}/merge")
    @PreAuthorize("hasAnyRole('PROPERTY','GOVERNANCE','ADMIN')")
    public BucketPoint merge(@PathVariable Long id, @RequestBody MergeRequest req) {
        return bucketPointService.merge(id, req.intoId());
    }

    /** 上报桶点满溢。 */
    @PostMapping("/{id}/overflow")
    @PreAuthorize("hasAnyRole('SUPERVISOR','PROPERTY','ADMIN')")
    public BucketEvent overflow(@PathVariable Long id) {
        return bucketPointService.reportOverflow(id, currentUser.require());
    }
}
