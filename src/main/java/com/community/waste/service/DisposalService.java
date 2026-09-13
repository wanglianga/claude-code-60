package com.community.waste.service;

import com.community.waste.config.AppProperties;
import com.community.waste.exception.ApiException;
import com.community.waste.model.*;
import com.community.waste.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class DisposalService {

    private final DisposalRecordRepo disposalRepo;
    private final BucketPointRepo bucketPointRepo;
    private final AppUserRepo userRepo;
    private final ReviewTaskRepo reviewTaskRepo;
    private final CollectionRecordRepo collectionRepo;
    private final PointsService pointsService;
    private final EventService eventService;
    private final AppProperties props;

    public DisposalService(DisposalRecordRepo disposalRepo, BucketPointRepo bucketPointRepo,
                           AppUserRepo userRepo, ReviewTaskRepo reviewTaskRepo,
                           CollectionRecordRepo collectionRepo, PointsService pointsService,
                           EventService eventService, AppProperties props) {
        this.disposalRepo = disposalRepo;
        this.bucketPointRepo = bucketPointRepo;
        this.userRepo = userRepo;
        this.reviewTaskRepo = reviewTaskRepo;
        this.collectionRepo = collectionRepo;
        this.pointsService = pointsService;
        this.eventService = eventService;
        this.props = props;
    }

    public record CreateRequest(Long userId, Long bucketPointId, String category, BigDecimal weightKg,
                                String photoUrl, Boolean bagBroken, Boolean obviousMissort,
                                List<String> detectedIssues, String source, String note) {
    }

    /**
     * 登记一次投放。居民扫码自助登记；督导员可代录（老人不会扫码）。
     * 摄像/督导员识别到误投问题时生成复核任务。
     */
    @Transactional
    public DisposalRecord create(AppUser operator, CreateRequest req) {
        if (req.weightKg() == null || req.weightKg().compareTo(BigDecimal.ZERO) <= 0) {
            throw ApiException.badRequest("重量必须大于 0");
        }
        DisposalRecord.Category category;
        try {
            category = DisposalRecord.Category.valueOf(req.category());
        } catch (Exception e) {
            throw ApiException.badRequest("垃圾类别不合法: " + req.category());
        }
        BucketPoint point = bucketPointRepo.findById(req.bucketPointId())
                .orElseThrow(() -> ApiException.notFound("桶点不存在"));
        if (point.getStatus() == BucketPoint.Status.MERGED) {
            throw ApiException.conflict("该桶点已撤桶并点，请前往 " +
                    (point.getMergedInto() == null ? "合并后的桶点" : point.getMergedInto().getName()) + " 投放");
        }
        if (point.getStatus() != BucketPoint.Status.ACTIVE) {
            throw ApiException.conflict("该桶点已关闭");
        }

        // 投放人：默认为操作者本人；督导员/物业可代录（老人不会扫码）
        AppUser disposer = operator;
        boolean proxy = false;
        if (req.userId() != null && !req.userId().equals(operator.getId())) {
            if (operator.getRole() != AppUser.Role.SUPERVISOR && operator.getRole() != AppUser.Role.PROPERTY
                    && operator.getRole() != AppUser.Role.ADMIN) {
                throw ApiException.forbidden("只有督导员/物业可以代录投放");
            }
            disposer = userRepo.findById(req.userId())
                    .orElseThrow(() -> ApiException.notFound("居民不存在"));
            proxy = true;
        }

        DisposalRecord record = new DisposalRecord();
        record.setUser(disposer);
        record.setBucketPoint(point);
        record.setCategory(category);
        record.setWeightKg(req.weightKg());
        record.setPhotoUrl(req.photoUrl());
        record.setBagBroken(Boolean.TRUE.equals(req.bagBroken()));
        record.setObviousMissort(Boolean.TRUE.equals(req.obviousMissort()));
        record.setProxyEntry(proxy);
        record.setNote(req.note());
        if (operator.getRole() == AppUser.Role.SUPERVISOR) {
            record.setSupervisor(operator);
        }

        // 定时投放窗口提示（不强制拦截，记录备注）
        String timeNote = outsideOpenWindow(point) ? "非定时投放时段登记" : null;

        // 积分：明显误投不加分，待复核
        boolean missort = record.isObviousMissort();
        int points = missort ? 0 : computePoints(category, req.weightKg(), record.isBagBroken());
        record.setPointsAwarded(points);
        record.setStatus(missort ? DisposalRecord.Status.UNDER_REVIEW : DisposalRecord.Status.RECORDED);
        if (timeNote != null) {
            record.setNote((req.note() == null ? "" : req.note() + "；") + timeNote);
        }
        DisposalRecord saved = disposalRepo.save(record);

        if (points > 0) {
            pointsService.apply(disposer, points, PointsTransaction.TxType.DISPOSAL_AWARD,
                    "DISPOSAL", saved.getId(),
                    category + " 投放 " + req.weightKg() + "kg" + (record.isBagBroken() ? "（破袋加分）" : ""));
        }

        // 生成复核任务：明显误投 + 摄像/督导识别的问题
        ReviewTask.Source source = "CAMERA".equalsIgnoreCase(req.source() == null ? "" : req.source())
                ? ReviewTask.Source.CAMERA : ReviewTask.Source.SUPERVISOR;
        if (missort) {
            createTask(saved, ReviewTask.IssueType.OBVIOUS_MISSORT, source);
        }
        if (req.detectedIssues() != null) {
            for (String issue : req.detectedIssues()) {
                ReviewTask.IssueType type;
                try {
                    type = ReviewTask.IssueType.valueOf(issue);
                } catch (Exception e) {
                    throw ApiException.badRequest("未知的问题类型: " + issue);
                }
                createTask(saved, type, source);
            }
        }
        if (!reviewTaskRepo.findByDisposalId(saved.getId()).isEmpty()) {
            saved.setStatus(DisposalRecord.Status.UNDER_REVIEW);
        }

        // 桶点满溢自动检测：自上次清运后累计投放量超过容量
        checkOverflow(point);
        return saved;
    }

    private boolean outsideOpenWindow(BucketPoint point) {
        try {
            LocalTime now = LocalTime.now();
            LocalTime start = LocalTime.parse(point.getOpenStart());
            LocalTime end = LocalTime.parse(point.getOpenEnd());
            return now.isBefore(start) || now.isAfter(end);
        } catch (Exception e) {
            return false;
        }
    }

    private int computePoints(DisposalRecord.Category category, BigDecimal weightKg, boolean bagBroken) {
        int perKg = props.getPoints().getPerKg().getOrDefault(category.name(), 1);
        int base = (int) Math.round(perKg * weightKg.doubleValue());
        if (bagBroken && category == DisposalRecord.Category.KITCHEN) {
            base += props.getPoints().getBagBrokenBonus();
        }
        return Math.max(base, 1);
    }

    private void createTask(DisposalRecord disposal, ReviewTask.IssueType type, ReviewTask.Source source) {
        ReviewTask task = new ReviewTask();
        task.setDisposal(disposal);
        task.setIssueType(type);
        task.setSource(source);
        task.setDueAt(OffsetDateTime.now().plusHours(props.getRules().getReviewSlaHours()));
        // 派单：优先投放登记的督导员，否则任一督导员
        AppUser assignee = disposal.getSupervisor();
        if (assignee == null) {
            assignee = userRepo.findByRole(AppUser.Role.SUPERVISOR).stream().findFirst().orElse(null);
        }
        task.setAssignee(assignee);
        reviewTaskRepo.save(task);
    }

    private void checkOverflow(BucketPoint point) {
        OffsetDateTime since = collectionRepo
                .findFirstByBucketPointIdAndArrivedAtIsNotNullOrderByArrivedAtDesc(point.getId())
                .map(c -> c.getArrivedAt())
                .orElse(point.getCreatedAt());
        BigDecimal accumulated = disposalRepo.sumWeightByPointSince(point.getId(), since);
        if (accumulated.compareTo(point.getCapacityKg()) >= 0) {
            eventService.raise(BucketEvent.Type.OVERFLOW, point, point.getBuilding(),
                    point.getName() + " 桶点满溢",
                    String.format("自上次清运后累计投放 %.1fkg，已达到容量 %.1fkg，请尽快安排清运。",
                            accumulated.doubleValue(), point.getCapacityKg().doubleValue()),
                    "OVERFLOW:" + point.getId(), null);
        }
    }

    @Transactional(readOnly = true)
    public List<DisposalRecord> listMine(AppUser user) {
        return disposalRepo.findByUserIdOrderByDisposedAtDesc(user.getId());
    }

    @Transactional(readOnly = true)
    public List<DisposalRecord> listAll() {
        return disposalRepo.findAll();
    }
}
