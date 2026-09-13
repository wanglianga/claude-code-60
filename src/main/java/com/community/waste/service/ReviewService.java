package com.community.waste.service;

import com.community.waste.config.AppProperties;
import com.community.waste.exception.ApiException;
import com.community.waste.model.*;
import com.community.waste.repo.DisposalRecordRepo;
import com.community.waste.repo.ReviewTaskRepo;
import com.community.waste.repo.ViolationRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class ReviewService {

    private final ReviewTaskRepo taskRepo;
    private final DisposalRecordRepo disposalRepo;
    private final ViolationRepo violationRepo;
    private final PointsService pointsService;
    private final AppProperties props;

    public ReviewService(ReviewTaskRepo taskRepo, DisposalRecordRepo disposalRepo,
                         ViolationRepo violationRepo, PointsService pointsService, AppProperties props) {
        this.taskRepo = taskRepo;
        this.disposalRepo = disposalRepo;
        this.violationRepo = violationRepo;
        this.pointsService = pointsService;
        this.props = props;
    }

    @Transactional(readOnly = true)
    public List<ReviewTask> listPending(AppUser user, boolean onlyMine) {
        if (onlyMine) {
            return taskRepo.findByAssigneeIdAndStatusOrderByCreatedAtAsc(user.getId(), ReviewTask.Status.PENDING);
        }
        return taskRepo.findByStatusOrderByCreatedAtAsc(ReviewTask.Status.PENDING);
    }

    @Transactional(readOnly = true)
    public List<ReviewTask> listAll() {
        return taskRepo.findAll();
    }

    /**
     * 完成复核。
     *
     * @param confirmed true=确认误投（扣分或教育提醒）；false=误报（撤销，必要时补发积分）
     * @param action    EDUCATION / DEDUCT（首次违规强制教育提醒）
     */
    @Transactional
    public ReviewTask complete(Long taskId, AppUser reviewer, boolean confirmed, String action, String note) {
        ReviewTask task = taskRepo.findById(taskId).orElseThrow(() -> ApiException.notFound("复核任务不存在"));
        if (task.getStatus() != ReviewTask.Status.PENDING) {
            throw ApiException.conflict("该任务已复核");
        }
        DisposalRecord disposal = task.getDisposal();
        AppUser resident = disposal.getUser();
        task.setReviewer(reviewer);
        task.setReviewedAt(OffsetDateTime.now());
        task.setNote(note);

        if (confirmed) {
            task.setStatus(ReviewTask.Status.CONFIRMED);
            // 首次违规 -> 教育提醒；再次违规 -> 扣分
            long priorViolations = violationRepo.countByUserIdAndCreatedAtAfter(
                    resident.getId(), OffsetDateTime.now().minusDays(90));
            ReviewTask.Action act;
            try {
                act = ReviewTask.Action.valueOf(action == null ? "DEDUCT" : action);
            } catch (Exception e) {
                throw ApiException.badRequest("action 只能是 EDUCATION 或 DEDUCT");
            }
            if (priorViolations == 0) {
                act = ReviewTask.Action.EDUCATION;
            }
            task.setAction(act);

            Violation violation = new Violation();
            violation.setUser(resident);
            violation.setReviewTaskId(task.getId());
            violation.setDisposalId(disposal.getId());
            if (act == ReviewTask.Action.DEDUCT) {
                int deduct = props.getPoints().getReviewDeduct();
                task.setDeductPoints(deduct);
                pointsService.apply(resident, -deduct, PointsTransaction.TxType.REVIEW_DEDUCT,
                        "REVIEW_TASK", task.getId(), "复核确认误投扣分：" + task.getIssueType());
                violation.setLevel(Violation.Level.MINOR);
                violation.setPointsDeducted(deduct);
                violation.setMessage("误投确认，扣减 " + deduct + " 分：" + task.getIssueType());
            } else {
                violation.setLevel(Violation.Level.EDUCATION);
                violation.setMessage("教育提醒：请正确分类投放（" + issueText(task.getIssueType()) + "）。首次违规不扣分，再次违规将扣分。");
            }
            violationRepo.save(violation);
            disposal.setStatus(DisposalRecord.Status.REJECTED);
        } else {
            task.setStatus(ReviewTask.Status.REJECTED);
            task.setAction(ReviewTask.Action.NONE);
            // 误报：若该投放因明显误投被冻结积分且没有其他待复核任务，补发积分
            boolean hasOtherOpen = taskRepo.findByDisposalId(disposal.getId()).stream()
                    .anyMatch(t -> t.getStatus() == ReviewTask.Status.PENDING && !t.getId().equals(task.getId()));
            if (!hasOtherOpen) {
                disposal.setStatus(DisposalRecord.Status.CONFIRMED);
                if (disposal.getPointsAwarded() == 0) {
                    int perKg = props.getPoints().getPerKg().getOrDefault(disposal.getCategory().name(), 1);
                    int points = Math.max(1, (int) Math.round(perKg * disposal.getWeightKg().doubleValue()));
                    disposal.setPointsAwarded(points);
                    pointsService.apply(resident, points, PointsTransaction.TxType.DISPOSAL_AWARD,
                            "DISPOSAL", disposal.getId(), "复核误报，补发投放积分");
                }
            }
        }
        disposalRepo.save(disposal);
        return taskRepo.save(task);
    }

    public static String issueText(ReviewTask.IssueType type) {
        return switch (type) {
            case PLASTIC_IN_KITCHEN -> "塑料袋混入厨余垃圾";
            case BATTERY_IN_OTHER -> "电池混入其他垃圾";
            case CARDBOARD_NOT_FLATTENED -> "纸箱未压扁";
            case CONTAINER_NOT_CLEANED -> "餐盒未清洗";
            case OBVIOUS_MISSORT -> "明显误投";
        };
    }
}
