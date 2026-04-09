-- ============================================================
-- Velona AI — 결제/구독 관련 테이블 (토스페이먼츠 연동)
-- 실행: MySQL crown_db
-- ============================================================

-- 1. 구독 정보
CREATE TABLE IF NOT EXISTS sm_subscription (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  member_id       INT NOT NULL,
  plan            ENUM('free','pro','team','business') NOT NULL DEFAULT 'free',
  billing_cycle   ENUM('monthly','yearly') DEFAULT NULL,
  status          ENUM('active','cancelled','expired','pending') NOT NULL DEFAULT 'active',
  billing_key     VARCHAR(255) DEFAULT NULL         COMMENT '토스페이먼츠 빌링키',
  customer_key    VARCHAR(255) DEFAULT NULL         COMMENT '토스페이먼츠 고객키',
  started_at      DATETIME NOT NULL,
  next_billing_at DATETIME DEFAULT NULL,
  cancelled_at    DATETIME DEFAULT NULL,
  cancel_reason   VARCHAR(100) DEFAULT NULL,
  expires_at      DATETIME DEFAULT NULL,
  created_at      DATETIME NOT NULL DEFAULT NOW(),
  updated_at      DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
  UNIQUE KEY uk_member (member_id),
  INDEX idx_status (status),
  INDEX idx_next_billing (next_billing_at)
) DEFAULT CHARSET=utf8mb4 COMMENT='구독 정보';

-- 2. 결제 내역
CREATE TABLE IF NOT EXISTS sm_payment (
  id                INT AUTO_INCREMENT PRIMARY KEY,
  member_id         INT NOT NULL,
  subscription_id   INT DEFAULT NULL,
  payment_key       VARCHAR(255) NOT NULL            COMMENT '토스페이먼츠 결제키',
  order_id          VARCHAR(255) NOT NULL            COMMENT '주문번호 (UUID)',
  order_name        VARCHAR(500) NOT NULL            COMMENT '상품명',
  amount            INT NOT NULL                     COMMENT '결제 금액 (원)',
  status            ENUM('pending','done','cancelled','failed','refunded') NOT NULL,
  method            VARCHAR(50) DEFAULT NULL          COMMENT '결제 수단',
  card_company      VARCHAR(50) DEFAULT NULL,
  card_number_last4 VARCHAR(10) DEFAULT NULL,
  receipt_url       VARCHAR(500) DEFAULT NULL,
  requested_at      DATETIME NOT NULL,
  approved_at       DATETIME DEFAULT NULL,
  cancelled_at      DATETIME DEFAULT NULL,
  raw_response      JSON DEFAULT NULL                COMMENT '토스페이먼츠 원본 응답',
  created_at        DATETIME NOT NULL DEFAULT NOW(),
  UNIQUE KEY uk_order_id (order_id),
  INDEX idx_member (member_id),
  INDEX idx_subscription (subscription_id),
  INDEX idx_status (status)
) DEFAULT CHARSET=utf8mb4 COMMENT='결제 내역';

-- 3. 사용량 추적 (플랜별 한도 체크)
CREATE TABLE IF NOT EXISTS sm_usage (
  id                INT AUTO_INCREMENT PRIMARY KEY,
  member_id         INT NOT NULL,
  year_month        CHAR(7) NOT NULL                 COMMENT '2026-04 형식',
  shorts_count      INT NOT NULL DEFAULT 0,
  longform_count    INT NOT NULL DEFAULT 0,
  voice_clone_count INT NOT NULL DEFAULT 0,
  ai_rewrite_count  INT NOT NULL DEFAULT 0,
  created_at        DATETIME NOT NULL DEFAULT NOW(),
  updated_at        DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
  UNIQUE KEY uk_member_month (member_id, year_month),
  INDEX idx_member (member_id)
) DEFAULT CHARSET=utf8mb4 COMMENT='월별 사용량';
