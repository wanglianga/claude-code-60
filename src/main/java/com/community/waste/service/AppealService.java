package com.community.waste.service;

import com.community.waste.exception.ApiException;
import com.community.waste.model.*;
import com.community.waste.repo.AppealRepo;
import com.community.waste.repo.PointsTransactionRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AppealService {

    private final AppealRepo appealRepo;
    private final PointsTransactionRepo txRepo;
    private final PointsService pointsService;
    private final EventService eventService;

    public AppealService(AppealRepo appealRepo, PointsTransactionRepo txRepo,
                         PointsService pointsService, EventService eventService) {
        this.appealRepo = appealRepo;
        this.txRepo = txRepo;
        this.pointsService = pointsService;
        this.eventService = eventService;
    }

    /** 居民对扣分流水发起申诉。 */
    @Transactional
    public Appeal file(AppUser user, Long pointsTransactionId, String reason) {
        PointsTransaction tx = txRepo.findById(pointsTransactionId)
                .orElseThrow(() -> ApiException.notFound("积分流水不存在"));
        if (!tx.getUser().getId().equals(user.getId())) {
            throw ApiException.forbidden("只能申诉自己的扣分记录");
        }
        if (tx.getType() != PointsTransaction.TxType.REVIEW_DEDUCT) {
            throw ApiException.badRequest("仅复核扣分记录可申诉");
        }
        if (appealRepo.existsByPointsTransactionIdAndStatus(pointsTransactionId, Appeal.Status.PENDING)) {
            throw ApiException.conflict("该记录已有待处理申诉");
        }
        Appeal appeal = new Appeal();
        appeal.setUser(user);
        appeal.setPointsTransactionId(pointsTransactionId);
        appeal.setReason(reason);
        Appeal saved = appealRepo.save(appeal);

        // 申诉进入桶点事件，串起居民与社区治理人员
        eventService.raise(BucketEvent.Type.APPEAL_FILED, null,
                user.getBuilding(),
                user.getDisplayName() + " 申诉扣分",
                String.format("居民 %s 对扣分流水#%d（%d 分）发起申诉：%s",
                        user.getDisplayName(), tx.getId(), tx.getDelta(), reason),
                "APPEAL:" + saved.getId(),
                List.of(user));
        return saved;
    }

    /** 社区治理人员处理申诉。 */
    @Transactional
    public Appeal handle(Long appealId, AppUser handler, boolean approve, String note) {
        Appeal appeal = appealRepo.findById(appealId).orElseThrow(() -> ApiException.notFound("申诉不存在"));
        if (appeal.getStatus() != Appeal.Status.PENDING) {
            throw ApiException.conflict("申诉已处理");
        }
        appeal.setHandler(handler);
        appeal.setHandledAt(OffsetDateTime.now());
        appeal.setHandleNote(note);
        if (approve) {
            appeal.setStatus(Appeal.Status.APPROVED);
            PointsTransaction tx = txRepo.findById(appeal.getPointsTransactionId()).orElseThrow();
            pointsService.apply(appeal.getUser(), -tx.getDelta(), PointsTransaction.TxType.APPEAL_RESTORE,
                    "APPEAL", appeal.getId(), "申诉通过，返还扣分");
        } else {
            appeal.setStatus(Appeal.Status.REJECTED);
        }
        return appealRepo.save(appeal);
    }

    @Transactional(readOnly = true)
    public List<Appeal> listPending() {
        return appealRepo.findByStatusOrderByCreatedAtDesc(Appeal.Status.PENDING);
    }

    @Transactional(readOnly = true)
    public List<Appeal> listMine(AppUser user) {
        return appealRepo.findByUserIdOrderByCreatedAtDesc(user.getId());
    }
}
