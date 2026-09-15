package com.community.waste.service;

import com.community.waste.model.AppUser;
import com.community.waste.model.PointsTransaction;
import com.community.waste.repo.AppUserRepo;
import com.community.waste.repo.PointsTransactionRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

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

    /** 从单个用户扣减（不超过余额），返回实际扣掉的分数。 */
    @Transactional(propagation = Propagation.REQUIRED)
    public int deductFrom(AppUser user, int amount, PointsTransaction.TxType type,
                          String refType, Long refId, String note) {
        AppUser managed = userRepo.findById(user.getId()).orElseThrow();
        int paid = Math.min(managed.getPointsBalance(), Math.max(0, amount));
        if (paid > 0) {
            apply(managed, -paid, type, refType, refId, note);
        }
        return paid;
    }

    /** 先扣本人，不足部分家庭成员共享代付（余额高者优先），返回总扣减额（可能不足 amount）。 */
    @Transactional(propagation = Propagation.REQUIRED)
    public int deductWithFamilyShare(AppUser user, int amount,
                                     PointsTransaction.TxType ownType, PointsTransaction.TxType shareType,
                                     String refType, Long refId, String note) {
        int paid = deductFrom(user, amount, ownType, refType, refId, note);
        int remaining = amount - paid;
        if (remaining > 0 && user.getFamily() != null) {
            List<AppUser> members = userRepo.findByFamilyId(user.getFamily().getId()).stream()
                    .filter(m -> !m.getId().equals(user.getId()))
                    .sorted(Comparator.comparingInt(AppUser::getPointsBalance).reversed())
                    .toList();
            for (AppUser member : members) {
                if (remaining <= 0) {
                    break;
                }
                int p = deductFrom(member, remaining, shareType, refType, refId, note + "（家庭共享代付）");
                paid += p;
                remaining -= p;
            }
        }
        return paid;
    }

    /** 按引用退回某业务对象的全部扣款（负 delta 流水原路返还）。 */
    @Transactional(propagation = Propagation.REQUIRED)
    public void refundByRef(String refType, Long refId, PointsTransaction.TxType refundType, String note) {
        for (PointsTransaction tx : txRepo.findByRefTypeAndRefId(refType, refId)) {
            if (tx.getDelta() < 0) {
                apply(tx.getUser(), -tx.getDelta(), refundType, refType, refId, note);
            }
        }
    }
}
