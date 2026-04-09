-- ============================================================
-- Velona AI — 토큰 기반 사용량 관리 + 구독 테이블 확장
-- 7개 플랜 체계 (개인4 + 팀2 + 비즈니스3)
-- 실행: MySQL crown_db
-- ============================================================

-- 1. 구독 테이블 플랜 ENUM 확장 + 만료일
ALTER TABLE sm_subscription
  MODIFY COLUMN plan ENUM(
    'free','pro','plus','max',
    'team_pro','team_max',
    'biz_pro','biz_plus','biz_max'
  ) NOT NULL DEFAULT 'free';

-- 2. 토큰 지갑 (멤버별 현재 잔량 + 만료일)
CREATE TABLE IF NOT EXISTS sm_token_wallet (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  member_id       INT NOT NULL,
  balance         INT NOT NULL DEFAULT 0              COMMENT '현재 잔여 토큰',
  granted_monthly INT NOT NULL DEFAULT 0              COMMENT '이번 달 충전된 토큰',
  used_monthly    INT NOT NULL DEFAULT 0              COMMENT '이번 달 사용한 토큰',
  year_month      CHAR(7) NOT NULL                    COMMENT '2026-04 형식',
  expires_at      DATETIME NOT NULL                   COMMENT '토큰 만료일 (결제 주기 종료일)',
  created_at      DATETIME NOT NULL DEFAULT NOW(),
  updated_at      DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
  UNIQUE KEY uk_member_month (member_id, year_month),
  INDEX idx_member (member_id),
  INDEX idx_expires (expires_at)
) DEFAULT CHARSET=utf8mb4 COMMENT='토큰 지갑 (월별)';

-- 3. 토큰 사용 이력 (충전/차감 원장)
CREATE TABLE IF NOT EXISTS sm_token_ledger (
  id              BIGINT AUTO_INCREMENT PRIMARY KEY,
  member_id       INT NOT NULL,
  type            ENUM('grant','use','refund','expire','bonus') NOT NULL COMMENT '충전/사용/환불/만료/보너스',
  amount          INT NOT NULL                        COMMENT '양수=충전, 음수=차감',
  balance_after   INT NOT NULL DEFAULT 0              COMMENT '처리 후 잔액',
  description     VARCHAR(200) DEFAULT NULL            COMMENT '사유 (영상 생성, 대본 AI 등)',
  project_id      INT DEFAULT NULL                    COMMENT '관련 프로젝트',
  expires_at      DATETIME DEFAULT NULL               COMMENT '이 토큰의 만료일',
  created_at      DATETIME NOT NULL DEFAULT NOW(),
  INDEX idx_member (member_id),
  INDEX idx_member_created (member_id, created_at),
  INDEX idx_type (type),
  INDEX idx_expires (expires_at)
) DEFAULT CHARSET=utf8mb4 COMMENT='토큰 충전/차감 이력';

-- 4. 플랜별 토큰 설정 (코드 하드코딩 대신 DB 관리)
CREATE TABLE IF NOT EXISTS sm_plan_config (
  id              INT AUTO_INCREMENT PRIMARY KEY,
  plan_id         VARCHAR(20) NOT NULL                COMMENT 'free, pro, plus, max, team_pro, ...',
  category        ENUM('personal','team','business') NOT NULL,
  name            VARCHAR(50) NOT NULL,
  monthly_price   INT NOT NULL DEFAULT 0              COMMENT '월 가격 (원)',
  yearly_price    INT NOT NULL DEFAULT 0              COMMENT '연 가격 (원)',
  tokens_monthly  INT NOT NULL DEFAULT 0              COMMENT '월 지급 토큰',
  per_member      TINYINT(1) NOT NULL DEFAULT 0       COMMENT '멤버당 과금 여부',
  min_members     INT NOT NULL DEFAULT 1              COMMENT '최소 멤버 수',
  max_members     INT NOT NULL DEFAULT 1              COMMENT '최대 멤버 수 (-1=무제한)',
  -- 기능 권한
  collaboration   TINYINT(1) NOT NULL DEFAULT 0       COMMENT '협업 기능',
  role_management TINYINT(1) NOT NULL DEFAULT 0       COMMENT '역할 관리 (owner/editor/viewer)',
  comments        TINYINT(1) NOT NULL DEFAULT 0       COMMENT '코멘트 기능',
  approval_workflow TINYINT(1) NOT NULL DEFAULT 0     COMMENT '승인 워크플로우',
  batch_automation TINYINT(1) NOT NULL DEFAULT 0      COMMENT '배치 자동화',
  priority_render TINYINT(1) NOT NULL DEFAULT 0       COMMENT '우선 렌더링',
  api_access      TINYINT(1) NOT NULL DEFAULT 0       COMMENT 'API 접근',
  watermark       TINYINT(1) NOT NULL DEFAULT 1       COMMENT '워터마크 포함 여부',
  white_label     TINYINT(1) NOT NULL DEFAULT 0       COMMENT '화이트라벨/브랜딩',
  invoice_support TINYINT(1) NOT NULL DEFAULT 0       COMMENT '세금계산서 발행',
  max_video_quality VARCHAR(10) NOT NULL DEFAULT '720p' COMMENT '720p/1080p/4k',
  support_level   ENUM('faq','email','email_priority','dedicated') NOT NULL DEFAULT 'faq',
  active          TINYINT(1) NOT NULL DEFAULT 1,
  created_at      DATETIME NOT NULL DEFAULT NOW(),
  updated_at      DATETIME NOT NULL DEFAULT NOW() ON UPDATE NOW(),
  UNIQUE KEY uk_plan_id (plan_id)
) DEFAULT CHARSET=utf8mb4 COMMENT='플랜 설정';

-- 5. 초기 플랜 데이터 삽입
INSERT INTO sm_plan_config
  (plan_id, category, name, monthly_price, yearly_price, tokens_monthly, per_member, min_members, max_members,
   collaboration, role_management, comments, approval_workflow, batch_automation, priority_render, api_access, watermark, white_label, invoice_support, max_video_quality, support_level)
VALUES
  -- 개인
  ('free',      'personal', 'Free',          0,      0,      50,   0, 1, 1,   0, 0, 0, 0, 0, 0, 0, 1, 0, 0, '720p',  'faq'),
  ('pro',       'personal', 'Pro',       14000, 140000,    600,   0, 1, 1,   0, 0, 0, 0, 0, 0, 0, 0, 0, 0, '1080p', 'email'),
  ('plus',      'personal', 'Plus',      26000, 260000,   1400,   0, 1, 1,   0, 0, 0, 0, 1, 1, 0, 0, 0, 0, '1080p', 'email'),
  ('max',       'personal', 'Max',       45000, 450000,   3500,   0, 1, 1,   0, 0, 0, 0, 1, 1, 1, 0, 0, 0, '4k',    'email_priority'),
  -- 팀
  ('team_pro',  'team',     'Team Pro',  18000, 180000,    900,   1, 2, 10,  1, 1, 1, 0, 0, 0, 0, 0, 0, 0, '1080p', 'email_priority'),
  ('team_max',  'team',     'Team Max',  33000, 330000,   2200,   1, 2, 10,  1, 1, 1, 0, 1, 1, 0, 0, 0, 0, '1080p', 'email_priority'),
  -- 비즈니스
  ('biz_pro',   'business', 'Biz Pro',   22000, 220000,   1600,   1, 5, 50,  1, 1, 1, 1, 1, 1, 0, 0, 0, 1, '1080p', 'dedicated'),
  ('biz_plus',  'business', 'Biz Plus',  36000, 360000,   3200,   1, 5, 100, 1, 1, 1, 1, 1, 1, 1, 0, 0, 1, '4k',    'dedicated'),
  ('biz_max',   'business', 'Biz Max',   49000, 490000,   5000,   1, 5, -1,  1, 1, 1, 1, 1, 1, 1, 0, 1, 1, '4k',    'dedicated')
ON DUPLICATE KEY UPDATE
  monthly_price = VALUES(monthly_price),
  yearly_price = VALUES(yearly_price),
  tokens_monthly = VALUES(tokens_monthly),
  updated_at = NOW();
