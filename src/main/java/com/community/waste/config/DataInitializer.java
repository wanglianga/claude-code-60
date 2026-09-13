package com.community.waste.config;

import com.community.waste.model.*;
import com.community.waste.repo.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.*;

/**
 * 首次启动时初始化演示数据：楼栋/家庭/用户/桶点/商品/活动/政策 + 80 天历史投放。
 */
@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final BuildingRepo buildingRepo;
    private final FamilyRepo familyRepo;
    private final AppUserRepo userRepo;
    private final BucketPointRepo bucketPointRepo;
    private final DisposalRecordRepo disposalRepo;
    private final PointsTransactionRepo txRepo;
    private final ProductRepo productRepo;
    private final CampaignRepo campaignRepo;
    private final PolicyRepo policyRepo;
    private final ReviewTaskRepo reviewTaskRepo;
    private final ViolationRepo violationRepo;
    private final CollectionRecordRepo collectionRepo;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(BuildingRepo buildingRepo, FamilyRepo familyRepo, AppUserRepo userRepo,
                           BucketPointRepo bucketPointRepo, DisposalRecordRepo disposalRepo,
                           PointsTransactionRepo txRepo, ProductRepo productRepo, CampaignRepo campaignRepo,
                           PolicyRepo policyRepo, ReviewTaskRepo reviewTaskRepo, ViolationRepo violationRepo,
                           CollectionRecordRepo collectionRepo, PasswordEncoder passwordEncoder) {
        this.buildingRepo = buildingRepo;
        this.familyRepo = familyRepo;
        this.userRepo = userRepo;
        this.bucketPointRepo = bucketPointRepo;
        this.disposalRepo = disposalRepo;
        this.txRepo = txRepo;
        this.productRepo = productRepo;
        this.campaignRepo = campaignRepo;
        this.policyRepo = policyRepo;
        this.reviewTaskRepo = reviewTaskRepo;
        this.violationRepo = violationRepo;
        this.collectionRepo = collectionRepo;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (userRepo.count() > 0) {
            return;
        }
        log.info("初始化演示数据...");
        Random rnd = new Random(20260913L);

        // 楼栋
        Building b1 = buildingRepo.save(new Building("B1", "1号楼"));
        Building b2 = buildingRepo.save(new Building("B2", "2号楼"));
        Building b3 = buildingRepo.save(new Building("B3", "3号楼"));

        // 家庭
        Family zhang = familyRepo.save(new Family("张家", b1, "1-101"));
        Family li = familyRepo.save(new Family("李家", b1, "1-102"));
        Family wang = familyRepo.save(new Family("王家", b2, "2-201"));
        Family zhao = familyRepo.save(new Family("赵家", b3, "3-301"));

        // 用户
        AppUser admin = user("admin", "Admin@123", "系统管理员", AppUser.Role.ADMIN, null, null, null);
        AppUser resident1 = user("resident1", "Resident@123", "张阿姨", AppUser.Role.RESIDENT, zhang, b1, "1-101");
        AppUser resident2 = user("resident2", "Resident@123", "李大爷", AppUser.Role.RESIDENT, li, b1, "1-102");
        resident2.setElderly(true);
        userRepo.save(resident2);
        AppUser resident3 = user("resident3", "Resident@123", "李小哥", AppUser.Role.RESIDENT, li, b1, "1-102");
        AppUser resident4 = user("resident4", "Resident@123", "王奶奶", AppUser.Role.RESIDENT, wang, b2, "2-201");
        AppUser resident5 = user("resident5", "Resident@123", "赵先生", AppUser.Role.RESIDENT, zhao, b3, "3-301");
        AppUser supervisor1 = user("supervisor1", "Supervisor@123", "陈督导", AppUser.Role.SUPERVISOR, null, b1, null);
        AppUser supervisor2 = user("supervisor2", "Supervisor@123", "刘督导", AppUser.Role.SUPERVISOR, null, b2, null);
        user("property1", "Property@123", "王经理(物业)", AppUser.Role.PROPERTY, null, null, null);
        user("collector1", "Collector@123", "赵师傅(清运)", AppUser.Role.COLLECTOR, null, null, null);
        user("governance1", "Governance@123", "周主任(社区)", AppUser.Role.GOVERNANCE, null, null, null);

        // 桶点
        BucketPoint bp1 = point("BP-01", "1号楼东桶点", b1, "1号楼东侧", 120);
        BucketPoint bp2 = point("BP-02", "2号楼桶点", b2, "2号楼北侧", 200);
        BucketPoint bp3 = point("BP-03", "3号楼桶点", b3, "3号楼垃圾房", 80);

        // 商品（米面油、垃圾袋、社区服务 + 一个已过期商品）
        product("五常大米 5kg", Product.Category.GOODS, 200, 20, 2, 4, null);
        product("金龙鱼食用油 4L", Product.Category.GOODS, 260, 15, 1, 2, null);
        product("分类垃圾袋(30只/卷)", Product.Category.GOODS, 50, 100, 5, 10, null);
        product("社区家政服务 1 小时", Product.Category.SERVICE, 500, 10, 1, 1, null);
        product("端午香囊(已过期)", Product.Category.GOODS, 30, 5, 3, 6, OffsetDateTime.now().minusDays(60));

        // 宣传活动
        Campaign campaign = new Campaign();
        campaign.setTitle("垃圾分类宣传周");
        campaign.setDescription("分类知识讲座 + 现场演示，到场奖励 20 积分");
        campaign.setStartAt(OffsetDateTime.now().minusDays(2));
        campaign.setEndAt(OffsetDateTime.now().plusDays(7));
        campaign.setCapacity(50);
        campaign.setPointsReward(20);
        campaignRepo.save(campaign);

        // 政策
        policy("3号楼撤桶并点试点", Policy.Type.BUCKET_MERGE, "3号楼桶点并入 1号楼东桶点，引导集中投放",
                LocalDate.now().minusDays(15), b3, null);
        policy("1号楼定时投放", Policy.Type.TIMED_DISPOSAL, "1号楼桶点每日 06:30-09:00、18:00-21:00 开放",
                LocalDate.now().minusDays(40), b1, null);
        policy("积分双倍周", Policy.Type.POINTS_CAMPAIGN, "厨余投放积分双倍，持续一周",
                LocalDate.now().minusDays(10), null, null);

        // 80 天历史投放（含误投），让月报/政策对比/事件巡检有数据
        List<AppUser> residents = List.of(resident1, resident2, resident3, resident4, resident5);
        Map<AppUser, BucketPoint> home = Map.of(
                resident1, bp1, resident2, bp1, resident3, bp1, resident4, bp2, resident5, bp3);
        Map<Long, Integer> balances = new HashMap<>();
        DisposalRecord.Category[] cats = DisposalRecord.Category.values();
        Map<DisposalRecord.Category, Integer> perKg = Map.of(
                DisposalRecord.Category.KITCHEN, 2, DisposalRecord.Category.RECYCLABLE, 3,
                DisposalRecord.Category.HAZARDOUS, 5, DisposalRecord.Category.OTHER, 1);

        for (int day = 80; day >= 0; day--) {
            for (AppUser r : residents) {
                if (rnd.nextDouble() > 0.45) {
                    continue;
                }
                DisposalRecord.Category cat = cats[rnd.nextInt(cats.length)];
                double weight = 0.3 + rnd.nextDouble() * 2.7;
                // 3号楼误投率偏高（演示误投率升高事件）
                double missortProb = r.getId().equals(resident5.getId()) ? 0.3 : 0.06;
                boolean missort = rnd.nextDouble() < missortProb;
                DisposalRecord d = new DisposalRecord();
                d.setUser(r);
                d.setBucketPoint(home.get(r));
                d.setCategory(cat);
                d.setWeightKg(BigDecimal.valueOf(Math.round(weight * 10.0) / 10.0));
                d.setPhotoUrl("/photos/seed-" + day + "-" + r.getId() + ".jpg");
                d.setDisposedAt(OffsetDateTime.now().minusDays(day).minusHours(rnd.nextInt(12)));
                d.setBagBroken(cat == DisposalRecord.Category.KITCHEN && rnd.nextBoolean());
                d.setObviousMissort(missort);
                d.setStatus(missort ? DisposalRecord.Status.REJECTED : DisposalRecord.Status.CONFIRMED);
                int points = missort ? 0 : Math.max(1, (int) Math.round(perKg.get(cat) * weight));
                d.setPointsAwarded(points);
                disposalRepo.save(d);
                if (points > 0) {
                    int bal = balances.merge(r.getId(), points, Integer::sum);
                    tx(r, points, bal, PointsTransaction.TxType.DISPOSAL_AWARD, "DISPOSAL", d.getId(),
                            "历史投放奖励", d.getDisposedAt());
                }
            }
        }

        // 赵先生 3 次 MINOR 违规（演示兑换拦截）
        for (int i = 0; i < 3; i++) {
            Violation v = new Violation();
            v.setUser(resident5);
            v.setLevel(Violation.Level.MINOR);
            v.setPointsDeducted(10);
            v.setMessage("误投确认，扣减 10 分：塑料袋混入厨余垃圾");
            v.setCreatedAt(OffsetDateTime.now().minusDays(10 - i));
            violationRepo.save(v);
            int bal = balances.merge(resident5.getId(), -10, Integer::sum);
            tx(resident5, -10, bal, PointsTransaction.TxType.REVIEW_DEDUCT, "VIOLATION", v.getId(),
                    "历史违规扣分", v.getCreatedAt());
        }
        // 王奶奶 1 条教育提醒
        Violation edu = new Violation();
        edu.setUser(resident4);
        edu.setLevel(Violation.Level.EDUCATION);
        edu.setMessage("教育提醒：请正确分类投放（纸箱未压扁）。首次违规不扣分。");
        edu.setCreatedAt(OffsetDateTime.now().minusDays(3));
        violationRepo.save(edu);

        // 一条超时未复核的任务（演示 REVIEW_SLOW 巡检）
        DisposalRecord pending = new DisposalRecord();
        pending.setUser(resident1);
        pending.setBucketPoint(bp1);
        pending.setCategory(DisposalRecord.Category.KITCHEN);
        pending.setWeightKg(new BigDecimal("1.5"));
        pending.setObviousMissort(true);
        pending.setStatus(DisposalRecord.Status.UNDER_REVIEW);
        pending.setPointsAwarded(0);
        pending.setDisposedAt(OffsetDateTime.now().minusDays(2));
        disposalRepo.save(pending);
        ReviewTask overdue = new ReviewTask();
        overdue.setDisposal(pending);
        overdue.setIssueType(ReviewTask.IssueType.PLASTIC_IN_KITCHEN);
        overdue.setSource(ReviewTask.Source.CAMERA);
        overdue.setAssignee(supervisor1);
        overdue.setCreatedAt(OffsetDateTime.now().minusDays(2));
        overdue.setDueAt(OffsetDateTime.now().minusDays(1));
        reviewTaskRepo.save(overdue);

        // 一班已过期未到达的清运车（演示 TRUCK_LATE 巡检）
        CollectionRecord late = new CollectionRecord();
        late.setBucketPoint(bp2);
        late.setTruckNo("沪A-10086");
        late.setScheduledAt(OffsetDateTime.now().minusHours(5));
        collectionRepo.save(late);
        // 一班已完成清运（作为桶点累计投放的基准）
        CollectionRecord done = new CollectionRecord();
        done.setBucketPoint(bp1);
        done.setTruckNo("沪A-10086");
        done.setScheduledAt(OffsetDateTime.now().minusDays(1));
        done.setArrivedAt(OffsetDateTime.now().minusDays(1).plusMinutes(10));
        done.setWeightKg(new BigDecimal("95.5"));
        done.setExpectedWeightKg(new BigDecimal("95.5"));
        collectionRepo.save(done);

        // 写回余额
        balances.forEach((uid, bal) -> userRepo.findById(uid).ifPresent(u -> {
            u.setPointsBalance(bal);
            userRepo.save(u);
        }));
        log.info("演示数据初始化完成：{} 条投放记录", disposalRepo.count());
    }

    private AppUser user(String username, String password, String name, AppUser.Role role,
                         Family family, Building building, String roomNo) {
        AppUser u = new AppUser();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode(password));
        u.setDisplayName(name);
        u.setRole(role);
        u.setFamily(family);
        u.setBuilding(building);
        u.setRoomNo(roomNo);
        return userRepo.save(u);
    }

    private BucketPoint point(String code, String name, Building building, String address, int capacityKg) {
        BucketPoint p = new BucketPoint();
        p.setCode(code);
        p.setName(name);
        p.setBuilding(building);
        p.setAddress(address);
        p.setCapacityKg(BigDecimal.valueOf(capacityKg));
        return bucketPointRepo.save(p);
    }

    private void product(String name, Product.Category category, int cost, int stock,
                         int userLimit, int familyLimit, OffsetDateTime validTo) {
        Product p = new Product();
        p.setName(name);
        p.setCategory(category);
        p.setCostPoints(cost);
        p.setStock(stock);
        p.setPerUserMonthlyLimit(userLimit);
        p.setPerFamilyMonthlyLimit(familyLimit);
        p.setValidFrom(OffsetDateTime.now().minusDays(90));
        p.setValidTo(validTo);
        productRepo.save(p);
    }

    private void policy(String name, Policy.Type type, String desc, LocalDate start, Building building, BucketPoint point) {
        Policy p = new Policy();
        p.setName(name);
        p.setType(type);
        p.setDescription(desc);
        p.setStartDate(start);
        p.setBuilding(building);
        p.setBucketPoint(point);
        policyRepo.save(p);
    }

    private void tx(AppUser user, int delta, int balanceAfter, PointsTransaction.TxType type,
                    String refType, Long refId, String note, OffsetDateTime at) {
        PointsTransaction t = new PointsTransaction();
        t.setUser(user);
        t.setFamily(user.getFamily());
        t.setDelta(delta);
        t.setBalanceAfter(balanceAfter);
        t.setType(type);
        t.setRefType(refType);
        t.setRefId(refId);
        t.setNote(note);
        t.setCreatedAt(at);
        txRepo.save(t);
    }
}
