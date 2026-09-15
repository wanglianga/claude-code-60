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
     * 记一笔积分流水并更新余额。用户行加悲观锁，并发扣减串行化；扣分不会扣到负数。
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public PointsTransaction apply(AppUser user, int delta, PointsTransaction.TxType type,
                                   String refType, Long refId, String note) {
        AppUser managed = userRepo.findByIdForUpdate(user.getId()).orElseThrow();
        return applyLocked(managed, delta, type, refType, refId, note);
    }

    /** 在已锁定的用户实体上记账（调用方需已通过行锁读取该用户）。 */
    private PointsTransaction applyLocked(AppUser managed, int delta, PointsTransaction.TxType type,
                                          String refType, Long refId, String note) {
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

    /** 从单个用户扣减（行锁保护，不超过余额），返回实际扣掉的分数。 */
    @Transactional(propagation = Propagation.REQUIRED)
    public int deductFrom(AppUser user, int amount, PointsTransaction.TxType type,
                          String refType, Long refId, String note) {
        AppUser managed = userRepo.findByIdForUpdate(user.getId()).orElseThrow();
        return deductFromLocked(managed, amount, type, refType, refId, note);
    }

    private int deductFromLocked(AppUser managed, int amount, PointsTransaction.TxType type,
                                 String refType, Long refId, String note) {
        int paid = Math.min(managed.getPointsBalance(), Math.max(0, amount));
        if (paid > 0) {
            applyLocked(managed, -paid, type, refType, refId, note);
        }
        return paid;
    }

    /**
     * 先扣本人，不足部分家庭成员共享代付（余额高者优先）。
     * 整个家庭按 id 排序一次加锁，避免并发代付死锁与余额丢失更新。
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public int deductWithFamilyShare(AppUser user, int amount,
                                     PointsTransaction.TxType ownType, PointsTransaction.TxType shareType,
                                     String refType, Long refId, String note) {
        List<AppUser> scope = user.getFamily() == null
                ? List.of(userRepo.findByIdForUpdate(user.getId()).orElseThrow())
                : userRepo.findByFamilyIdForUpdate(user.getFamily().getId());
        AppUser self = scope.stream().filter(u -> u.getId().equals(user.getId())).findFirst().orElseThrow();

        int paid = deductFromLocked(self, amount, ownType, refType, refId, note);
        int remaining = amount - paid;
        if (remaining > 0) {
            List<AppUser> others = scope.stream()
                    .filter(u -> !u.getId().equals(self.getId()))
                    .sorted(Comparator.comparingInt(AppUser::getPointsBalance).reversed())
                    .toList();
            for (AppUser member : others) {
                if (remaining <= 0) {
                    break;
                }
                int p = deductFromLocked(member, remaining, shareType, refType, refId, note + "（家庭共享代付）");
                paid += p;
                remaining -= p;
            }
        }
        return paid;
    }

    /**
     * 按引用退回某业务对象的全部扣款（负 delta 流水原路返还）。
     * 幂等：同一对象已存在 refundType 流水时跳过，防止并发/重试导致重复退款。
     */
    @Transactional(propagation = Propagation.REQUIRED)
    public void refundByRef(String refType, Long refId, PointsTransaction.TxType refundType, String note) {
        if (txRepo.existsByRefTypeAndRefIdAndType(refType, refId, refundType)) {
            return;
        }
        for (PointsTransaction tx : txRepo.findByRefTypeAndRefId(refType, refId)) {
            if (tx.getDelta() < 0) {
                apply(tx.getUser(), -tx.getDelta(), refundType, refType, refId, note);
            }
        }
    }
}
