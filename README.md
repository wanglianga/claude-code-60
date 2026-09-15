# 城市垃圾分类驿站 · 积分兑换与误投追踪服务

基于 **Java 17 + Spring Boot 3 + PostgreSQL 16** 的社区垃圾分类治理服务：覆盖居民投放登记、摄像/督导误投识别与复核、积分增减与教育提醒、积分商城兑换（库存/限购/家庭共享/违规拦截）、**兑换库存联动（缺货预约排队、到货自动分配、到期回滚、替代商品、采购计划）**、桶点事件多方联动、清运称重异常、申诉处理、宣传活动、月度治理报告与政策前后对比。

## 原始需求

> 开发城市垃圾分类驿站积分兑换与误投追踪服务，可采用 Java、Spring Boot 和 PostgreSQL。居民投放厨余、可回收、有害和其他垃圾时，服务记录用户、桶点、重量、照片、投放时间、督导员、是否破袋和是否有明显误投。摄像或督导员识别到塑料袋混入厨余、电池混入其他垃圾、纸箱未压扁或餐盒未清洗时，服务生成复核任务。督导员复核后，居民积分增加、扣减或进入教育提醒；积分兑换米面油、垃圾袋或社区服务时，服务检查库存、兑换限制、家庭成员共享和历史违规。若某栋楼误投率升高、某督导员复核过慢、桶点满溢、清运车未按时到达或居民申诉扣分，服务要把居民、督导、物业、清运公司和社区治理人员串到同一桶点事件。月度分析时，积分、误投、清运量、兑换成本和宣传活动效果进入小区治理报告，用于调整桶点开放时间和督导排班。服务还要处理老人不会扫码、督导员代录、清运称重异常、积分商品过期和社区宣传活动报名，使分类治理从一次投放延伸到居民习惯改变。对撤桶并点、定时投放和积分活动，服务要能比较政策前后的居民参与度和误投变化，支撑社区继续推广或调整。

## 快速开始（Docker 一键部署）

前置：已安装 Docker 与 Docker Compose v2。

```bash
cp .env.example .env        # 按需修改端口/密码/密钥
docker compose up -d --build
```

启动后（app 健康检查通过即可）：

- 演示页面（浏览器）：`http://localhost:${CC_PUBLISH_PORT}/`
- 健康检查：`http://localhost:${CC_PUBLISH_PORT}/actuator/health`
- 停止：`docker compose down`（加 `-v` 同时清空数据）

说明：compose 中仅 app 服务发布到宿主端口（`${CC_PUBLISH_PORT}:8080`），PostgreSQL 只在内部网络通过服务名 `db` 访问。首次启动会自动建表（Flyway）并写入演示数据（约 80 天历史投放、商品、活动、政策、一条超时复核任务与一班迟到清运车，便于立即演示事件巡检）。

**验证方式 = 宿主 docker compose up**：本服务以「`docker compose up -d` 健康 + 关键业务流在浏览器/接口走通」为验证通过标准，无需在宿主机安装 JDK/Maven。

## 测试账号（演示数据）

| 角色 | 用户名 | 密码 | 权限说明 |
|---|---|---|---|
| 居民 | `resident1` | `Resident@123` | 张阿姨，1号楼张家。投放、兑换、申诉、报名活动 |
| 居民(老人) | `resident2` | `Resident@123` | 李大爷，1号楼李家，老人标记，可由督导员代录 |
| 居民 | `resident3` | `Resident@123` | 李小哥，与李大爷同家庭（演示家庭积分共享代付） |
| 居民 | `resident4` | `Resident@123` | 王奶奶，2号楼（有一条教育提醒记录） |
| 居民(违规) | `resident5` | `Resident@123` | 赵先生，3号楼，90 天内 3 次违规（演示兑换拦截） |
| 督导员 | `supervisor1` | `Supervisor@123` | 陈督导，1号楼。代录投放、复核任务、上报满溢 |
| 督导员 | `supervisor2` | `Supervisor@123` | 刘督导，2号楼 |
| 物业 | `property1` | `Property@123` | 王经理。商品管理、订单核销、桶点管理、清运排班 |
| 清运公司 | `collector1` | `Collector@123` | 赵师傅。清运排班、到达称重 |
| 社区治理 | `governance1` | `Governance@123` | 周主任。申诉处理、活动管理、月报、政策对比 |
| 管理员 | `admin` | `Admin@123` | 全部权限 + 人工调整积分 |

## 关键业务流（可在演示页面逐一点击，或用 curl）

1. **投放登记**：`POST /api/disposals`（居民扫码；督导员传 `userId` 即代录；`detectedIssues` 模拟摄像头识别塑料袋混入厨余/电池混入其他垃圾/纸箱未压扁/餐盒未清洗 → 自动生成复核任务；`obviousMissort=true` 冻结积分待复核）
2. **复核**：`GET /api/review-tasks` → `POST /api/review-tasks/{id}/complete`（确认误投：首次→教育提醒，再次→扣分；判定误报：补发积分）
3. **兑换**：`POST /api/redemptions`（校验库存、个人/家庭月度限购、90 天违规拦截；本人积分不足时家庭成员共享代付）；物业 `POST /api/redemptions/{id}/fulfill` 核销
3.1 **兑换库存联动（排队）**：库存不足时 `POST /api/queue` 预约排队（冻结积分，返回位次/预计到货/替代商品）；`POST /api/restock-plans` 纳入采购计划 → `POST /api/restock-plans/{id}/arrive` 到货自动按序分配；`POST /api/queue/{id}/pickup` 实际领取；到期未领自动回滚退积分（定时任务 + `POST /api/queue/process-expiries` 手动触发）；`POST /api/queue/{id}/alt-pickup` 领取替代商品（消耗积分、原预约保留排队）；`GET /api/queue/suggestions` 高需求商品采购建议
4. **事件联动**：`POST /api/events/check` 手动巡检（复核超时、清运车迟到、楼栋误投率升高），满溢可人工 `POST /api/bucket-points/{id}/overflow` 或投放累计超容量自动触发；事件自动挂接居民/督导/物业/清运/治理人员
5. **清运**：`POST /api/collections/schedule` 排班 → `POST /api/collections/{id}/arrive` 称重（与应收重量偏差 >20% 标记异常并生成事件）
6. **申诉**：居民 `POST /api/appeals` → 治理人员 `POST /api/appeals/{id}/handle`（通过则返还扣分）
7. **月报**：`GET /api/reports/monthly?month=2026-09`（积分/误投/清运量/兑换成本/活动效果 + 治理建议）
8. **政策对比**：`GET /api/policies/{id}/compare?days=30`（撤桶并点/定时投放/积分活动前后参与度与误投率）

## API 认证

`POST /api/auth/login` 获取 JWT（`{"username","password"}`），之后请求头带 `Authorization: Bearer <token>`。

## 工程结构

```
├── Dockerfile              # 多阶段构建（maven 构建 → jre 运行，非 root + HEALTHCHECK）
├── docker-compose.yml      # app + postgres（仅 app 发布宿主端口）
├── .env.example
└── src/main
    ├── java/com/community/waste
    │   ├── config/         # 安全/JWT、规则配置、种子数据
    │   ├── model/          # 17 个 JPA 实体（投放/复核/积分/兑换/事件/清运/申诉/活动/政策…）
    │   ├── repo/           # Spring Data JPA 仓库
    │   ├── service/        # 业务规则（积分、复核、兑换校验、事件巡检、月报、政策对比）
    │   └── web/            # REST 控制器（角色鉴权）
    └── resources
        ├── application.yml
        ├── db/migration/V1__schema.sql
        └── static/index.html   # 演示页面
```
