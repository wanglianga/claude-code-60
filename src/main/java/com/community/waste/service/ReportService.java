package com.community.waste.service;

import com.community.waste.model.*;
import com.community.waste.repo.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 月度小区治理报告：积分、误投、清运量、兑换成本、宣传活动效果。
 */
@Service
public class ReportService {

    private final DisposalRecordRepo disposalRepo;
    private final PointsTransactionRepo txRepo;
    private final RedemptionOrderRepo orderRepo;
    private final ViolationRepo violationRepo;
    private final CollectionRecordRepo collectionRepo;
    private final CampaignRepo campaignRepo;
    private final CampaignSignupRepo signupRepo;
    private final BuildingRepo buildingRepo;
    private final AppUserRepo userRepo;
    private final RedemptionReservationRepo reservationRepo;

    public ReportService(DisposalRecordRepo disposalRepo, PointsTransactionRepo txRepo,
                         RedemptionOrderRepo orderRepo, ViolationRepo violationRepo,
                         CollectionRecordRepo collectionRepo, CampaignRepo campaignRepo,
                         CampaignSignupRepo signupRepo, BuildingRepo buildingRepo, AppUserRepo userRepo,
                         RedemptionReservationRepo reservationRepo) {
        this.disposalRepo = disposalRepo;
        this.txRepo = txRepo;
        this.orderRepo = orderRepo;
        this.violationRepo = violationRepo;
        this.collectionRepo = collectionRepo;
        this.campaignRepo = campaignRepo;
        this.signupRepo = signupRepo;
        this.buildingRepo = buildingRepo;
        this.userRepo = userRepo;
        this.reservationRepo = reservationRepo;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> monthly(String month) {
        YearMonth ym = YearMonth.parse(month);
        ZoneId zone = ZoneId.systemDefault();
        OffsetDateTime from = ym.atDay(1).atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime to = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toOffsetDateTime();

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("month", ym.toString());

        // 投放与误投
        long disposals = disposalRepo.countByDisposedAtBetween(from, to);
        long missorts = disposalRepo.countMissortByRange(from, to);
        BigDecimal totalWeight = disposalRepo.sumWeightByRange(from, to);
        long activeResidents = disposalRepo.countDistinctUsersByRange(from, to);
        long totalResidents = userRepo.findByRole(AppUser.Role.RESIDENT).size();

        Map<String, Object> disposal = new LinkedHashMap<>();
        disposal.put("count", disposals);
        disposal.put("totalWeightKg", totalWeight);
        disposal.put("missortCount", missorts);
        disposal.put("missortRate", disposals == 0 ? 0 : round((double) missorts / disposals));
        disposal.put("activeResidents", activeResidents);
        disposal.put("participationRate", totalResidents == 0 ? 0 : round((double) activeResidents / totalResidents));
        List<Map<String, Object>> byCategory = new ArrayList<>();
        for (Object[] row : disposalRepo.statsByCategoryAndRange(from, to)) {
            Map<String, Object> c = new LinkedHashMap<>();
            c.put("category", row[0].toString());
            c.put("count", row[1]);
            c.put("weightKg", row[2]);
            byCategory.add(c);
        }
        disposal.put("byCategory", byCategory);
        report.put("disposal", disposal);

        // 积分
        Map<String, Object> points = new LinkedHashMap<>();
        long awarded = txRepo.sumDeltaByTypeAndRange(PointsTransaction.TxType.DISPOSAL_AWARD, from, to)
                + txRepo.sumDeltaByTypeAndRange(PointsTransaction.TxType.CAMPAIGN_REWARD, from, to);
        long deducted = txRepo.sumDeltaByTypeAndRange(PointsTransaction.TxType.REVIEW_DEDUCT, from, to);
        long redeemed = txRepo.sumDeltaByTypeAndRange(PointsTransaction.TxType.REDEMPTION, from, to)
                + txRepo.sumDeltaByTypeAndRange(PointsTransaction.TxType.REDEMPTION_SHARE, from, to);
        points.put("awarded", awarded);
        points.put("deducted", deducted);
        points.put("redeemed", -redeemed);
        report.put("points", points);

        // 楼栋维度
        List<Map<String, Object>> buildings = new ArrayList<>();
        for (Building b : buildingRepo.findAll()) {
            long cnt = disposalRepo.countByBuildingAndRange(b.getId(), from, to);
            long mis = disposalRepo.countMissortByBuildingAndRange(b.getId(), from, to);
            Map<String, Object> bm = new LinkedHashMap<>();
            bm.put("building", b.getName());
            bm.put("disposals", cnt);
            bm.put("missorts", mis);
            bm.put("missortRate", cnt == 0 ? 0 : round((double) mis / cnt));
            bm.put("weightKg", disposalRepo.sumWeightByBuildingAndRange(b.getId(), from, to));
            bm.put("activeResidents", disposalRepo.countDistinctUsersByBuildingAndRange(b.getId(), from, to));
            buildings.add(bm);
        }
        report.put("buildings", buildings);

        // 兑换成本
        Map<String, Object> redemption = new LinkedHashMap<>();
        redemption.put("orderCount", orderRepo.countByCreatedAtBetween(from, to));
        redemption.put("pointsCost", orderRepo.sumPointsSpentByRange(from, to));
        report.put("redemption", redemption);

        // 预约排队联动
        Map<String, Object> queue = new LinkedHashMap<>();
        queue.put("waiting", reservationRepo.countByStatus(RedemptionReservation.Status.WAITING));
        queue.put("ready", reservationRepo.countByStatus(RedemptionReservation.Status.READY));
        queue.put("fulfilled", reservationRepo.countByStatusAndFulfilledAtBetween(
                RedemptionReservation.Status.FULFILLED, from, to));
        queue.put("expired", reservationRepo.countByStatusAndCancelledAtBetween(
                RedemptionReservation.Status.EXPIRED, from, to));
        report.put("redemptionQueue", queue);

        // 清运
        List<CollectionRecord> collections = collectionRepo.findByScheduledAtBetween(from, to);
        BigDecimal collectedWeight = collections.stream()
                .map(c -> c.getWeightKg() == null ? BigDecimal.ZERO : c.getWeightKg())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        Map<String, Object> collection = new LinkedHashMap<>();
        collection.put("tripCount", collections.size());
        collection.put("arrivedCount", collections.stream().filter(c -> c.getArrivedAt() != null).count());
        collection.put("weightKg", collectedWeight);
        collection.put("anomalyCount", collections.stream().filter(CollectionRecord::isAnomaly).count());
        report.put("collection", collection);

        // 违规与申诉
        report.put("violations", violationRepo.countByCreatedAtBetween(from, to));

        // 宣传活动效果
        List<Map<String, Object>> campaigns = new ArrayList<>();
        for (Campaign c : campaignRepo.findAll()) {
            if (c.getEndAt().isBefore(from) || c.getStartAt().isAfter(to)) {
                continue;
            }
            Map<String, Object> cm = new LinkedHashMap<>();
            cm.put("title", c.getTitle());
            cm.put("signups", signupRepo.countByCampaignId(c.getId()));
            cm.put("attended", signupRepo.countByCampaignIdAndAttendedTrue(c.getId()));
            campaigns.add(cm);
        }
        report.put("campaigns", campaigns);

        // 治理建议（规则生成，用于调整桶点开放时间与督导排班）
        List<String> suggestions = new ArrayList<>();
        for (Map<String, Object> bm : buildings) {
            double rate = (double) bm.get("missortRate");
            if (rate >= 0.15 && (long) bm.get("disposals") >= 5) {
                suggestions.add(bm.get("building") + " 误投率 " + Math.round(rate * 100) + "% 偏高，建议增加该楼栋督导排班并开展入户宣传");
            }
        }
        if (disposals > 0 && (double) missorts / disposals < 0.05) {
            suggestions.add("全小区误投率低于 5%，分类习惯良好，可维持现有桶点开放时间");
        }
        long pendingTrips = collections.stream().filter(c -> c.getArrivedAt() == null).count();
        if (pendingTrips > 0) {
            suggestions.add("本月有 " + pendingTrips + " 次清运未按时完成，建议与清运公司核对排班");
        }
        if (campaigns.stream().mapToLong(c -> (long) c.get("attended")).sum() == 0 && !campaigns.isEmpty()) {
            suggestions.add("宣传活动到场率偏低，建议结合积分奖励提升参与度");
        }
        report.put("suggestions", suggestions);
        return report;
    }

    private static double round(double v) {
        return Math.round(v * 1000.0) / 1000.0;
    }
}
