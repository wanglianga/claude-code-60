-- 兑换库存联动：预约排队 + 供应商补货计划
CREATE TABLE redemption_reservations (
    id             BIGSERIAL PRIMARY KEY,
    user_id        BIGINT      NOT NULL REFERENCES app_users (id),
    family_id      BIGINT REFERENCES families (id),
    product_id     BIGINT      NOT NULL REFERENCES products (id),
    quantity       INT         NOT NULL,
    points_reserved INT        NOT NULL,
    status         VARCHAR(16) NOT NULL DEFAULT 'WAITING', -- WAITING/READY/FULFILLED/CANCELLED/EXPIRED
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    ready_at       TIMESTAMPTZ,  -- 到货分配时间
    expire_at      TIMESTAMPTZ,  -- 领取截止（到期未领自动回滚）
    fulfilled_at   TIMESTAMPTZ,  -- 实际领取时间
    cancelled_at   TIMESTAMPTZ,  -- 取消/过期关闭时间
    alt_product_id BIGINT REFERENCES products (id), -- 领取的替代商品
    alt_order_id   BIGINT,                          -- 替代商品兑换单
    alt_fulfilled_at TIMESTAMPTZ
);
CREATE INDEX idx_reservation_queue ON redemption_reservations (product_id, status, created_at);
CREATE INDEX idx_reservation_user ON redemption_reservations (user_id, created_at);
CREATE INDEX idx_reservation_expire ON redemption_reservations (status, expire_at);

CREATE TABLE restock_plans (
    id            BIGSERIAL PRIMARY KEY,
    product_id    BIGINT       NOT NULL REFERENCES products (id),
    quantity      INT          NOT NULL,
    expected_at   TIMESTAMPTZ  NOT NULL, -- 供应商预计到货时间
    arrived_at    TIMESTAMPTZ,
    status        VARCHAR(16)  NOT NULL DEFAULT 'PLANNED', -- PLANNED/ARRIVED/CANCELLED
    note          VARCHAR(512),
    created_by_id BIGINT REFERENCES app_users (id),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_restock_product ON restock_plans (product_id, status, expected_at);
