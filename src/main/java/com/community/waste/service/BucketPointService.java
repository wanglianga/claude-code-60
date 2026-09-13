package com.community.waste.service;

import com.community.waste.exception.ApiException;
import com.community.waste.model.AppUser;
import com.community.waste.model.BucketEvent;
import com.community.waste.model.BucketPoint;
import com.community.waste.repo.BucketPointRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class BucketPointService {

    private final BucketPointRepo bucketPointRepo;
    private final EventService eventService;

    public BucketPointService(BucketPointRepo bucketPointRepo, EventService eventService) {
        this.bucketPointRepo = bucketPointRepo;
        this.eventService = eventService;
    }

    @Transactional(readOnly = true)
    public List<BucketPoint> list() {
        return bucketPointRepo.findAll();
    }

    @Transactional
    public BucketPoint create(BucketPoint point) {
        bucketPointRepo.findByCode(point.getCode()).ifPresent(p -> {
            throw ApiException.conflict("桶点编码已存在: " + point.getCode());
        });
        return bucketPointRepo.save(point);
    }

    /** 撤桶并点：把 from 桶点合并到 into 桶点。 */
    @Transactional
    public BucketPoint merge(Long fromId, Long intoId) {
        if (fromId.equals(intoId)) {
            throw ApiException.badRequest("不能合并到自身");
        }
        BucketPoint from = bucketPointRepo.findById(fromId).orElseThrow(() -> ApiException.notFound("桶点不存在"));
        BucketPoint into = bucketPointRepo.findById(intoId).orElseThrow(() -> ApiException.notFound("目标桶点不存在"));
        if (from.getStatus() == BucketPoint.Status.MERGED) {
            throw ApiException.conflict("该桶点已合并");
        }
        from.setStatus(BucketPoint.Status.MERGED);
        from.setMergedInto(into);
        from.setMergedAt(OffsetDateTime.now());
        return bucketPointRepo.save(from);
    }

    /** 督导员/物业上报桶点满溢。 */
    @Transactional
    public BucketEvent reportOverflow(Long pointId, AppUser reporter) {
        BucketPoint point = bucketPointRepo.findById(pointId).orElseThrow(() -> ApiException.notFound("桶点不存在"));
        return eventService.raise(BucketEvent.Type.OVERFLOW, point, point.getBuilding(),
                point.getName() + " 桶点满溢（人工上报）",
                reporter.getDisplayName() + " 上报桶点满溢，请物业与清运公司尽快处理。",
                "OVERFLOW:" + point.getId(), List.of(reporter));
    }
}
