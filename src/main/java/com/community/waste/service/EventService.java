package com.community.waste.service;

import com.community.waste.config.AppProperties;
import com.community.waste.exception.ApiException;
import com.community.waste.model.*;
import com.community.waste.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 桶点事件中心：把居民、督导、物业、清运公司、社区治理人员串到同一桶点事件。
 */
@Service
public class EventService {

    private static final Logger log = LoggerFactory.getLogger(EventService.class);

    private final BucketEventRepo eventRepo;
    private final AppUserRepo userRepo;
    private final ReviewTaskRepo reviewTaskRepo;
    private final CollectionRecordRepo collectionRepo;
    private final DisposalRecordRepo disposalRepo;
    private final BuildingRepo buildingRepo;
    private final AppProperties props;

    public EventService(BucketEventRepo eventRepo, AppUserRepo userRepo, ReviewTaskRepo reviewTaskRepo,
                        CollectionRecordRepo collectionRepo, DisposalRecordRepo disposalRepo,
                        BuildingRepo buildingRepo, AppProperties props) {
        this.eventRepo = eventRepo;
        this.userRepo = userRepo;
        this.reviewTaskRepo = reviewTaskRepo;
        this.collectionRepo = collectionRepo;
        this.disposalRepo = disposalRepo;
        this.buildingRepo = buildingRepo;
        this.props = props;
    }

    /**
     * 创建事件并按类型挂接相关角色（去重：同一 dedupeKey 的未解决事件只保留一个）。
     */
    @Transactional
    public BucketEvent raise(BucketEvent.Type type, BucketPoint point, Building building,
                             String title, String detail, String dedupeKey, Collection<AppUser> extraParticipants) {
        if (dedupeKey != null) {
            Optional<BucketEvent> existing = eventRepo.findOpenByDedupeKey(dedupeKey);
            if (existing.isPresent()) {
                return existing.get();
            }
        }
        BucketEvent event = new BucketEvent();
        event.setType(type);
        event.setBucketPoint(point);
        event.setBuilding(building);
        event.setTitle(title);
        event.setDetail(detail);
        event.setDedupeKey(dedupeKey);

        Set<AppUser> participants = new LinkedHashSet<>();
        if (extraParticipants != null) {
            participants.addAll(extraParticipants);
        }
        switch (type) {
            case MISSORT_RATE_HIGH -> {
                participants.addAll(userRepo.findByRole(AppUser.Role.GOVERNANCE));
                participants.addAll(userRepo.findByRole(AppUser.Role.PROPERTY));
                participants.addAll(userRepo.findByRole(AppUser.Role.SUPERVISOR));
            }
            case REVIEW_SLOW -> participants.addAll(userRepo.findByRole(AppUser.Role.PROPERTY));
            case OVERFLOW -> {
                participants.addAll(userRepo.findByRole(AppUser.Role.PROPERTY));
                participants.addAll(userRepo.findByRole(AppUser.Role.COLLECTOR));
            }
            case TRUCK_LATE -> {
                participants.addAll(userRepo.findByRole(AppUser.Role.COLLECTOR));
                participants.addAll(userRepo.findByRole(AppUser.Role.PROPERTY));
            }
            case APPEAL_FILED -> participants.addAll(userRepo.findByRole(AppUser.Role.GOVERNANCE));
            case WEIGH_ANOMALY -> {
                participants.addAll(userRepo.findByRole(AppUser.Role.COLLECTOR));
                participants.addAll(userRepo.findByRole(AppUser.Role.PROPERTY));
                participants.addAll(userRepo.findByRole(AppUser.Role.GOVERNANCE));
            }
        }
        participants.stream()
                .filter(Objects::nonNull)
                .forEach(u -> event.getParticipants().add(new EventParticipant(event, u, u.getRole().name())));
        BucketEvent saved = eventRepo.save(event);
        log.info("桶点事件[{}] {} -> 参与方: {}", type, title,
                participants.stream().map(AppUser::getDisplayName).collect(Collectors.joining(",")));
        return saved;
    }

    @Transactional(readOnly = true)
    public List<BucketEvent> list(String status) {
        if (status == null || status.isBlank()) {
            return eventRepo.findByStatusNotOrderByCreatedAtDesc(BucketEvent.Status.RESOLVED);
        }
        return eventRepo.findByStatusOrderByCreatedAtDesc(BucketEvent.Status.valueOf(status));
    }

    @Transactional(readOnly = true)
    public List<BucketEvent> mine(AppUser user) {
        return eventRepo.findByParticipantUserId(user.getId());
    }

    @Transactional
    public BucketEvent acknowledge(Long id, AppUser operator) {
        BucketEvent event = eventRepo.findById(id).orElseThrow(() -> ApiException.notFound("事件不存在"));
        event.setStatus(BucketEvent.Status.ACKNOWLEDGED);
        return eventRepo.save(event);
    }

    @Transactional
    public BucketEvent resolve(Long id, AppUser operator) {
        BucketEvent event = eventRepo.findById(id).orElseThrow(() -> ApiException.notFound("事件不存在"));
        event.setStatus(BucketEvent.Status.RESOLVED);
        event.setResolvedAt(OffsetDateTime.now());
        event.setResolvedBy(operator);
        return eventRepo.save(event);
    }

    /** 定时巡检：复核超时、清运车迟到、楼栋误投率升高。 */
    @Scheduled(fixedDelayString = "${app.rules.watchdog-delay-ms:60000}", initialDelay = 30000)
    @Transactional
    public void watchdog() {
        runAllChecks();
    }

    /** 手动/定时触发全部巡检，返回新生成的事件（便于演示与验证）。 */
    @Transactional
    public List<BucketEvent> runAllChecks() {
        List<BucketEvent> created = new ArrayList<>();
        created.addAll(checkReviewSla());
        created.addAll(checkTruckLate());
        created.addAll(checkMissortRate());
        return created;
    }

    /** 督导员复核过慢：PENDING 且超过 SLA 的复核任务。 */
    public List<BucketEvent> checkReviewSla() {
        List<BucketEvent> created = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();
        for (ReviewTask task : reviewTaskRepo.findByStatusAndDueAtBefore(ReviewTask.Status.PENDING, now)) {
            AppUser assignee = task.getAssignee();
            String dedupe = "REVIEW_SLOW:" + task.getId();
            BucketEvent e = raise(BucketEvent.Type.REVIEW_SLOW,
                    task.getDisposal().getBucketPoint(),
                    task.getDisposal().getBucketPoint().getBuilding(),
                    "复核任务超时未处理",
                    String.format("复核任务#%d（%s）已超过 SLA %d 小时未处理，督导员：%s",
                            task.getId(), task.getIssueType(), props.getRules().getReviewSlaHours(),
                            assignee == null ? "未分配" : assignee.getDisplayName()),
                    dedupe,
                    assignee == null ? List.of() : List.of(assignee));
            if (e.getStatus() == BucketEvent.Status.OPEN && e.getCreatedAt().isAfter(now.minusMinutes(2))) {
                created.add(e);
            }
        }
        return created;
    }

    /** 清运车未按时到达。 */
    public List<BucketEvent> checkTruckLate() {
        List<BucketEvent> created = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();
        for (CollectionRecord rec : collectionRepo.findByArrivedAtIsNullAndScheduledAtBefore(now)) {
            String dedupe = "TRUCK_LATE:" + rec.getId();
            BucketEvent e = raise(BucketEvent.Type.TRUCK_LATE, rec.getBucketPoint(),
                    rec.getBucketPoint().getBuilding(),
                    "清运车未按时到达",
                    String.format("清运车 %s 应于 %s 到达桶点 %s，至今未到。",
                            rec.getTruckNo(), rec.getScheduledAt(), rec.getBucketPoint().getName()),
                    dedupe, List.of());
            if (e.getCreatedAt().isAfter(now.minusMinutes(2))) {
                created.add(e);
            }
        }
        return created;
    }

    /** 楼栋误投率升高（最近 7 天）。 */
    public List<BucketEvent> checkMissortRate() {
        List<BucketEvent> created = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime from = now.minusDays(7);
        for (Building building : buildingRepo.findAll()) {
            long total = disposalRepo.countByBuildingAndRange(building.getId(), from, now);
            if (total < props.getRules().getMissortMinDisposals()) {
                continue;
            }
            long missort = disposalRepo.countMissortByBuildingAndRange(building.getId(), from, now);
            double rate = (double) missort / total;
            if (rate >= props.getRules().getMissortRateThreshold()) {
                String dedupe = "MISSORT_RATE_HIGH:" + building.getId();
                BucketEvent e = raise(BucketEvent.Type.MISSORT_RATE_HIGH, null, building,
                        building.getName() + "误投率升高",
                        String.format("最近 7 天投放 %d 次，确认误投 %d 次，误投率 %.1f%%，超过阈值 %.0f%%。",
                                total, missort, rate * 100, props.getRules().getMissortRateThreshold() * 100),
                        dedupe, List.of());
                if (e.getCreatedAt().isAfter(now.minusMinutes(2))) {
                    created.add(e);
                }
            }
        }
        return created;
    }
}
