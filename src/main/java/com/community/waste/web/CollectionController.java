package com.community.waste.web;

import com.community.waste.config.CurrentUser;
import com.community.waste.model.CollectionRecord;
import com.community.waste.service.CollectionService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    private final CollectionService collectionService;
    private final CurrentUser currentUser;

    public CollectionController(CollectionService collectionService, CurrentUser currentUser) {
        this.collectionService = collectionService;
        this.currentUser = currentUser;
    }

    public record ScheduleRequest(@NotNull Long bucketPointId, @NotBlank String truckNo,
                                  @NotNull OffsetDateTime scheduledAt) {
    }

    @PostMapping("/schedule")
    @PreAuthorize("hasAnyRole('COLLECTOR','PROPERTY','ADMIN')")
    public CollectionRecord schedule(@RequestBody ScheduleRequest req) {
        return collectionService.schedule(currentUser.require(), req.bucketPointId(), req.truckNo(), req.scheduledAt());
    }

    public record ArriveRequest(@NotNull BigDecimal weightKg) {
    }

    /** 清运车到达称重，自动检测称重异常。 */
    @PostMapping("/{id}/arrive")
    @PreAuthorize("hasAnyRole('COLLECTOR','PROPERTY','ADMIN')")
    public CollectionRecord arrive(@PathVariable Long id, @RequestBody ArriveRequest req) {
        return collectionService.arrive(id, req.weightKg());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COLLECTOR','PROPERTY','GOVERNANCE','ADMIN')")
    public List<CollectionRecord> list(@RequestParam(required = false) Long bucketPointId) {
        return bucketPointId == null ? collectionService.listAll() : collectionService.listByPoint(bucketPointId);
    }
}
