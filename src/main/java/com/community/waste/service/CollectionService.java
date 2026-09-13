package com.community.waste.service;

import com.community.waste.config.AppProperties;
import com.community.waste.exception.ApiException;
import com.community.waste.model.*;
import com.community.waste.repo.BucketPointRepo;
import com.community.waste.repo.CollectionRecordRepo;
import com.community.waste.repo.DisposalRecordRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class CollectionService {

    private final CollectionRecordRepo collectionRepo;
    private final BucketPointRepo bucketPointRepo;
    private final DisposalRecordRepo disposalRepo;
    private final EventService eventService;
    private final AppProperties props;

    public CollectionService(CollectionRecordRepo collectionRepo, BucketPointRepo bucketPointRepo,
                             DisposalRecordRepo disposalRepo, EventService eventService, AppProperties props) {
        this.collectionRepo = collectionRepo;
        this.bucketPointRepo = bucketPointRepo;
        this.disposalRepo = disposalRepo;
        this.eventService = eventService;
        this.props = props;
    }

    /** 清运公司排班。 */
    @Transactional
    public CollectionRecord schedule(AppUser operator, Long bucketPointId, String truckNo, OffsetDateTime scheduledAt) {
        BucketPoint point = bucketPointRepo.findById(bucketPointId)
                .orElseThrow(() -> ApiException.notFound("桶点不存在"));
        CollectionRecord record = new CollectionRecord();
        record.setBucketPoint(point);
        record.setTruckNo(truckNo);
        record.setScheduledAt(scheduledAt);
        record.setCreatedBy(operator);
        return collectionRepo.save(record);
    }

    /**
     * 清运车到达并称重。称重与应收重量（自上次清运以来的投放累计）偏差过大时标记异常并生成事件。
     */
    @Transactional
    public CollectionRecord arrive(Long recordId, BigDecimal weightKg) {
        CollectionRecord record = collectionRepo.findById(recordId)
                .orElseThrow(() -> ApiException.notFound("清运记录不存在"));
        if (record.getArrivedAt() != null) {
            throw ApiException.conflict("该清运单已到达登记");
        }
        BucketPoint point = record.getBucketPoint();
        // 先确定应收基准（上次清运完成时间；首次则统计全部历史投放），再登记到达，避免本单被计入基准
        OffsetDateTime since = collectionRepo
                .findFirstByBucketPointIdAndArrivedAtIsNotNullOrderByArrivedAtDesc(point.getId())
                .map(CollectionRecord::getArrivedAt)
                .orElse(OffsetDateTime.now().minusYears(10));
        BigDecimal expected = disposalRepo.sumWeightByPointSince(point.getId(), since);

        record.setArrivedAt(OffsetDateTime.now());
        record.setWeightKg(weightKg);
        record.setExpectedWeightKg(expected);

        if (expected.compareTo(BigDecimal.ZERO) > 0 && weightKg != null) {
            BigDecimal diff = weightKg.subtract(expected).abs();
            BigDecimal ratio = diff.divide(expected, 4, RoundingMode.HALF_UP);
            if (ratio.doubleValue() > props.getRules().getWeighAnomalyRatio()) {
                record.setAnomaly(true);
                eventService.raise(BucketEvent.Type.WEIGH_ANOMALY, point, point.getBuilding(),
                        point.getName() + " 清运称重异常",
                        String.format("清运车 %s 称重 %.1fkg，应收约 %.1fkg，偏差 %.0f%%，请核查是否漏记投放或称重异常。",
                                record.getTruckNo(), weightKg.doubleValue(), expected.doubleValue(),
                                ratio.doubleValue() * 100),
                        "WEIGH_ANOMALY:" + record.getId(), null);
            }
        }
        return collectionRepo.save(record);
    }

    @Transactional(readOnly = true)
    public List<CollectionRecord> listByPoint(Long bucketPointId) {
        return collectionRepo.findByBucketPointIdOrderByScheduledAtDesc(bucketPointId);
    }

    @Transactional(readOnly = true)
    public List<CollectionRecord> listAll() {
        return collectionRepo.findAll();
    }
}
