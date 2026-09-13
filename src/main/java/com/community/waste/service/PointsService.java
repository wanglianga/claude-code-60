package com.community.waste.service;

import com.community.waste.model.AppUser;
import com.community.waste.model.PointsTransaction;
import com.community.waste.repo.AppUserRepo;
import com.community.waste.repo.PointsTransactionRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PointsService {

    private final PointsTransactionRepo txRepo;
    private final AppUserRepo userRepo;

    public PointsService(PointsTransactionRepo txRepo, AppUserRepo userRepo) {
        this.txRepo = txRepo;
        this.userRepo = userRepo;
    }

    /**
     * 记一笔积分流水并更新余额。扣分不会扣到负数（最多扣到 0）。
     *
     * @return 实际发生的流水（delta 可能与请求不同，例如余额不足时截断）
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public PointsTransaction apply(AppUser user, int delta, PointsTransaction.TxType type,
                                   String refType, Long refId, String note) {
        AppUser managed = userRepo.findById(user.getId()).orElseThrow();
        int actual = delta;
        if (actual < 0 && managed.getPointsBalance() + actual < 0) {
            actual = -managed.getPointsBalance();
        }
        if (actual == 0 && delta != 0) {
            // 余额为 0 且要扣分，记一条 0 流水便于审计
        }
        managed.setPointsBalance(managed.getPointsBalance() + actual);
        PointsTransaction tx = new PointsTransaction();
        tx.setUser(managed);
        tx.setFamily(managed.getFamily());
        tx.setDelta(actual);
        tx.setBalanceAfter(managed.getPointsBalance());
        tx.setType(type);
        tx.setRefType(refType);
        tx.setRefId(refId);
        tx.setNote(note);
        return txRepo.save(tx);
    }
}
