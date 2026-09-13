-- 城市垃圾分类驿站积分兑换与误投追踪服务 —— 数据库结构
CREATE TABLE buildings (
    id          BIGSERIAL PRIMARY KEY,
    code        VARCHAR(32)  NOT NULL UNIQUE,
    name        VARCHAR(128) NOT NULL
);

CREATE TABLE families (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(128) NOT NULL,
    building_id BIGINT       NOT NULL REFERENCES buildings (id),
    room_no     VARCHAR(32)  NOT NULL
);

CREATE TABLE app_users (
    id             BIGSERIAL PRIMARY KEY,
    username       VARCHAR(64)  NOT NULL UNIQUE,
    password       VARCHAR(128) NOT NULL,
    display_name   VARCHAR(64)  NOT NULL,
    role           VARCHAR(32)  NOT NULL, -- RESIDENT/SUPERVISOR/PROPERTY/COLLECTOR/GOVERNANCE/ADMIN
    phone          VARCHAR(32),
    family_id      BIGINT REFERENCES families (id),
    building_id    BIGINT REFERENCES buildings (id),
    room_no        VARCHAR(32),
    points_balance INT          NOT NULL DEFAULT 0,
    elderly        BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_app_users_family ON app_users (family_id);
CREATE INDEX idx_app_users_role ON app_users (role);

CREATE TABLE bucket_points (
    id           BIGSERIAL PRIMARY KEY,
    code         VARCHAR(32)  NOT NULL UNIQUE,
    name         VARCHAR(128) NOT NULL,
    building_id  BIGINT       NOT NULL REFERENCES buildings (id),
    address      VARCHAR(255),
    open_start   VARCHAR(8)   NOT NULL DEFAULT '06:30', -- 定时投放窗口
    open_end     VARCHAR(8)   NOT NULL DEFAULT '21:00',
    capacity_kg  NUMERIC(10, 2) NOT NULL DEFAULT 200,
    status       VARCHAR(16)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE/MERGED/CLOSED
    merged_into  BIGINT REFERENCES bucket_points (id),
    merged_at    TIMESTAMPTZ,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE disposal_records (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT        NOT NULL REFERENCES app_users (id),
    bucket_point_id BIGINT        NOT NULL REFERENCES bucket_points (id),
    category        VARCHAR(16)   NOT NULL, -- KITCHEN/RECYCLABLE/HAZARDOUS/OTHER
    weight_kg       NUMERIC(8, 2) NOT NULL,
    photo_url       VARCHAR(512),
    disposed_at     TIMESTAMPTZ   NOT NULL DEFAULT now(),
    supervisor_id   BIGINT REFERENCES app_users (id),
    bag_broken      BOOLEAN       NOT NULL DEFAULT FALSE,
    obvious_missort BOOLEAN       NOT NULL DEFAULT FALSE,
    proxy_entry     BOOLEAN       NOT NULL DEFAULT FALSE, -- 督导员代录（老人不会扫码）
    points_awarded  INT           NOT NULL DEFAULT 0,
    status          VARCHAR(16)   NOT NULL DEFAULT 'RECORDED', -- RECORDED/UNDER_REVIEW/CONFIRMED/REJECTED
    note            VARCHAR(512),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);
CREATE INDEX idx_disposal_user ON disposal_records (user_id, disposed_at);
CREATE INDEX idx_disposal_point ON disposal_records (bucket_point_id, disposed_at);
CREATE INDEX idx_disposal_status ON disposal_records (status);

CREATE TABLE review_tasks (
    id           BIGSERIAL PRIMARY KEY,
    disposal_id  BIGINT      NOT NULL REFERENCES disposal_records (id),
    issue_type   VARCHAR(40) NOT NULL, -- PLASTIC_IN_KITCHEN/BATTERY_IN_OTHER/CARDBOARD_NOT_FLATTENED/CONTAINER_NOT_CLEANED/OBVIOUS_MISSORT
    source       VARCHAR(16) NOT NULL, -- CAMERA/SUPERVISOR
    status       VARCHAR(16) NOT NULL DEFAULT 'PENDING', -- PENDING/CONFIRMED/REJECTED
    assignee_id  BIGINT REFERENCES app_users (id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    due_at       TIMESTAMPTZ NOT NULL,
    reviewed_at  TIMESTAMPTZ,
    reviewer_id  BIGINT REFERENCES app_users (id),
    action       VARCHAR(16), -- NONE/EDUCATION/DEDUCT
    deduct_points INT         NOT NULL DEFAULT 0,
    note         VARCHAR(512)
);
CREATE INDEX idx_review_status ON review_tasks (status, due_at);
CREATE INDEX idx_review_assignee ON review_tasks (assignee_id, status);

CREATE TABLE points_transactions (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT      NOT NULL REFERENCES app_users (id),
    family_id     BIGINT REFERENCES families (id),
    delta         INT         NOT NULL,
    balance_after INT         NOT NULL,
    type          VARCHAR(32) NOT NULL, -- DISPOSAL_AWARD/REVIEW_DEDUCT/REDEMPTION/REDEMPTION_SHARE/APPEAL_RESTORE/ADJUST/CAMPAIGN_REWARD
    ref_type      VARCHAR(32),
    ref_id        BIGINT,
    note          VARCHAR(512),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_points_tx_user ON points_transactions (user_id, created_at);
CREATE INDEX idx_points_tx_type ON points_transactions (type, created_at);

CREATE TABLE products (
    id                       BIGSERIAL PRIMARY KEY,
    name                     VARCHAR(128) NOT NULL,
    category                 VARCHAR(16)  NOT NULL DEFAULT 'GOODS', -- GOODS(米面油/垃圾袋)/SERVICE(社区服务)
    cost_points              INT          NOT NULL,
    stock                    INT          NOT NULL DEFAULT 0,
    per_user_monthly_limit   INT          NOT NULL DEFAULT 5,
    per_family_monthly_limit INT          NOT NULL DEFAULT 10,
    valid_from               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    valid_to                 TIMESTAMPTZ, -- NULL 表示长期有效；过期商品不可兑换
    active                   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE redemption_orders (
    id            BIGSERIAL PRIMARY KEY,
    order_no      VARCHAR(40) NOT NULL UNIQUE,
    user_id       BIGINT      NOT NULL REFERENCES app_users (id),
    family_id     BIGINT REFERENCES families (id),
    product_id    BIGINT      NOT NULL REFERENCES products (id),
    quantity      INT         NOT NULL,
    points_spent  INT         NOT NULL,
    status        VARCHAR(16) NOT NULL DEFAULT 'PENDING', -- PENDING/FULFILLED/CANCELLED/REJECTED
    reject_reason VARCHAR(255),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    fulfilled_at  TIMESTAMPTZ
);
CREATE INDEX idx_redeem_user ON redemption_orders (user_id, created_at);
CREATE INDEX idx_redeem_family ON redemption_orders (family_id, created_at);

CREATE TABLE violations (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT      NOT NULL REFERENCES app_users (id),
    review_task_id  BIGINT REFERENCES review_tasks (id),
    disposal_id     BIGINT REFERENCES disposal_records (id),
    level           VARCHAR(16) NOT NULL, -- EDUCATION/MINOR/MAJOR
    points_deducted INT         NOT NULL DEFAULT 0,
    message         VARCHAR(512),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_violation_user ON violations (user_id, created_at);

CREATE TABLE bucket_events (
    id              BIGSERIAL PRIMARY KEY,
    bucket_point_id BIGINT REFERENCES bucket_points (id),
    building_id     BIGINT REFERENCES buildings (id),
    type            VARCHAR(32) NOT NULL, -- MISSORT_RATE_HIGH/REVIEW_SLOW/OVERFLOW/TRUCK_LATE/APPEAL_FILED/WEIGH_ANOMALY
    status          VARCHAR(16) NOT NULL DEFAULT 'OPEN', -- OPEN/ACKNOWLEDGED/RESOLVED
    title           VARCHAR(255) NOT NULL,
    detail          VARCHAR(1024),
    dedupe_key      VARCHAR(255), -- 同一未决事件去重
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    resolved_at     TIMESTAMPTZ,
    resolved_by_id  BIGINT REFERENCES app_users (id)
);
CREATE INDEX idx_event_status ON bucket_events (status, type);
CREATE UNIQUE INDEX idx_event_dedupe ON bucket_events (dedupe_key) WHERE status <> 'RESOLVED';

CREATE TABLE event_participants (
    id         BIGSERIAL PRIMARY KEY,
    event_id   BIGINT      NOT NULL REFERENCES bucket_events (id),
    user_id    BIGINT      NOT NULL REFERENCES app_users (id),
    role       VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (event_id, user_id)
);

CREATE TABLE collection_records (
    id                 BIGSERIAL PRIMARY KEY,
    bucket_point_id    BIGINT      NOT NULL REFERENCES bucket_points (id),
    truck_no           VARCHAR(32) NOT NULL,
    company            VARCHAR(128) NOT NULL DEFAULT '城绿清运公司',
    scheduled_at       TIMESTAMPTZ NOT NULL,
    arrived_at         TIMESTAMPTZ,
    weight_kg          NUMERIC(10, 2),
    expected_weight_kg NUMERIC(10, 2),
    anomaly            BOOLEAN     NOT NULL DEFAULT FALSE,
    created_by_id      BIGINT REFERENCES app_users (id),
    created_at         TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_collection_point ON collection_records (bucket_point_id, scheduled_at);

CREATE TABLE appeals (
    id                   BIGSERIAL PRIMARY KEY,
    user_id              BIGINT      NOT NULL REFERENCES app_users (id),
    points_transaction_id BIGINT     NOT NULL REFERENCES points_transactions (id),
    reason               VARCHAR(512) NOT NULL,
    status               VARCHAR(16)  NOT NULL DEFAULT 'PENDING', -- PENDING/APPROVED/REJECTED
    handler_id           BIGINT REFERENCES app_users (id),
    handled_at           TIMESTAMPTZ,
    handle_note          VARCHAR(512),
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE campaigns (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(128) NOT NULL,
    description   VARCHAR(1024),
    start_at      TIMESTAMPTZ  NOT NULL,
    end_at        TIMESTAMPTZ  NOT NULL,
    capacity      INT          NOT NULL DEFAULT 50,
    points_reward INT          NOT NULL DEFAULT 0,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE campaign_signups (
    id          BIGSERIAL PRIMARY KEY,
    campaign_id BIGINT      NOT NULL REFERENCES campaigns (id),
    user_id     BIGINT      NOT NULL REFERENCES app_users (id),
    signed_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    attended    BOOLEAN     NOT NULL DEFAULT FALSE,
    attended_at TIMESTAMPTZ,
    UNIQUE (campaign_id, user_id)
);

CREATE TABLE policies (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(128) NOT NULL,
    type            VARCHAR(32)  NOT NULL, -- BUCKET_MERGE/TIMED_DISPOSAL/POINTS_CAMPAIGN
    description     VARCHAR(1024),
    start_date      DATE         NOT NULL,
    end_date        DATE,
    bucket_point_id BIGINT REFERENCES bucket_points (id),
    building_id     BIGINT REFERENCES buildings (id),
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
